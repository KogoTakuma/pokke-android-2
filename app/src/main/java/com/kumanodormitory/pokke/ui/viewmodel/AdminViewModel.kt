package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.SeedData
import android.content.SharedPreferences
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
import com.kumanodormitory.pokke.data.remote.dto.SyncPullRequest
import com.kumanodormitory.pokke.data.remote.dto.SyncPullParcelRequest
import com.kumanodormitory.pokke.data.remote.dto.SyncPullRyoseiRequest
import com.kumanodormitory.pokke.data.remote.dto.SyncPushRequest
import com.kumanodormitory.pokke.data.remote.dto.SyncPushParcelRequest
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val lostParcels: List<ParcelEntity> = emptyList(),
    val archivedParcels: List<ParcelEntity> = emptyList(),
    val showArchived: Boolean = false,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val healthStatus: HealthStatus = HealthStatus.UNKNOWN,
    val isCheckingHealth: Boolean = false,
    val isSyncingRyosei: Boolean = false,
    val isSyncingParcel: Boolean = false,
    val isUploadingAllParcels: Boolean = false,
    val isPullingAllParcels: Boolean = false,
    val lastRyoseiSyncAt: Long? = null,
    val lastParcelSyncAt: Long? = null,
    val errorLog: List<ErrorEntry> = emptyList()
)

enum class HealthStatus { UNKNOWN, OK, ERROR }

data class ErrorEntry(
    val timestamp: Long,
    val source: String,
    val message: String
)

class AdminViewModel(
    private val parcelRepository: ParcelRepository,
    private val ryoseiRepository: RyoseiRepository,
    private val operationLogRepository: OperationLogRepository,
    private val syncPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState(
        lastRyoseiSyncAt = syncPrefs.getLong("lastRyoseiSyncAt", 0L).takeIf { it > 0 },
        lastParcelSyncAt = syncPrefs.getLong("lastParcelSyncAt", 0L).takeIf { it > 0 }
    ))
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    companion object {
        private const val ADMIN_PASSWORD = "PassworD"
    }

    fun authenticate(password: String) {
        if (password == ADMIN_PASSWORD) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                passwordError = null
            )
            loadLostParcels()
        } else {
            _uiState.value = _uiState.value.copy(
                passwordError = "パスワードが正しくありません"
            )
        }
    }

    private fun loadLostParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                parcelRepository.getLostParcels().collect { parcels ->
                    _uiState.value = _uiState.value.copy(
                        lostParcels = parcels,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
        viewModelScope.launch {
            try {
                parcelRepository.getArchivedLostParcels().collect { parcels ->
                    _uiState.value = _uiState.value.copy(archivedParcels = parcels)
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleShowArchived() {
        _uiState.value = _uiState.value.copy(showArchived = !_uiState.value.showArchived)
    }

    fun confirmLost(parcelId: String) {
        viewModelScope.launch {
            try {
                parcelRepository.archiveLostParcels(listOf(parcelId))
                operationLogRepository.addLog(
                    type = "ARCHIVE_LOST",
                    parcelId = parcelId,
                    operatedByName = null,
                    metadata = "1件アーカイブ"
                )
                _uiState.value = _uiState.value.copy(snackbarMessage = "紛失確定しました")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "紛失確定に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun archiveLostParcels() {
        viewModelScope.launch {
            try {
                val ids = _uiState.value.lostParcels.map { it.id }
                if (ids.isEmpty()) return@launch
                parcelRepository.archiveLostParcels(ids)
                operationLogRepository.addLog(
                    type = "ARCHIVE_LOST",
                    parcelId = null,
                    operatedByName = null,
                    metadata = "${ids.size}件アーカイブ"
                )
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "${ids.size}件の紛失荷物をアーカイブしました"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "アーカイブに失敗しました: ${e.message}"
                )
            }
        }
    }

    fun generateSeedData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val seedRyoseiList = SeedData.buildSeedRyoseiList()
                ryoseiRepository.insertAll(seedRyoseiList)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータを${seedRyoseiList.size}件生成しました"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータ生成に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun deleteSeedData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                ryoseiRepository.deleteSeedData()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータを削除しました"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "シードデータ削除に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun syncRyosei() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingRyosei = true, snackbarMessage = null)
            try {
                val deviceId = syncPrefs.getString("deviceId", null) ?: run {
                    val id = "pokke-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                    syncPrefs.edit().putString("deviceId", id).apply()
                    id
                }
                val request = SyncPullRequest(
                    deviceId = deviceId,
                    parcels = SyncPullParcelRequest(mode = "SNAPSHOT"),
                    ryosei = SyncPullRyoseiRequest(mode = "SNAPSHOT")
                )
                val response = PokkeApiClient.service.syncPull(body = request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val entities = body.ryosei.items.map { dto ->
                            RyoseiEntity(
                                id = dto.id, name = dto.name, nameKana = dto.nameKana,
                                nameAlphabet = dto.nameAlphabet, room = dto.room,
                                block = dto.block, leavingDate = dto.leavingDate,
                                discordStatus = dto.discordStatus
                            )
                        }
                        ryoseiRepository.replaceAll(entities)
                        val now = System.currentTimeMillis()
                        syncPrefs.edit().putLong("lastRyoseiSyncAt", now).apply()
                        _uiState.value = _uiState.value.copy(
                            isSyncingRyosei = false,
                            lastRyoseiSyncAt = now,
                            snackbarMessage = "寮生データを${entities.size}件同期しました"
                        )
                    }
                } else {
                    val code = response.code()
                    val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    appendError("syncRyosei", "HTTP $code\nurl=${response.raw().request.url}\nbody=${errBody ?: "(empty)"}")
                    _uiState.value = _uiState.value.copy(
                        isSyncingRyosei = false,
                        snackbarMessage = "寮生同期失敗: HTTP $code"
                    )
                }
            } catch (e: Exception) {
                appendError("syncRyosei", "${e::class.simpleName}: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSyncingRyosei = false,
                    snackbarMessage = "寮生同期失敗: ${e.message}"
                )
            }
        }
    }

    fun syncParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingParcel = true, snackbarMessage = null)
            try {
                val unsyncedParcels = parcelRepository.getUnsyncedParcels(System.currentTimeMillis())
                if (unsyncedParcels.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSyncingParcel = false,
                        snackbarMessage = "未同期の荷物はありません"
                    )
                    return@launch
                }

                val deviceId = syncPrefs.getString("deviceId", null) ?: run {
                    val id = "pokke-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                    syncPrefs.edit().putString("deviceId", id).apply()
                    id
                }
                val dtos = unsyncedParcels.map { it.toSyncDto() }
                val request = SyncPushRequest(
                    deviceId = deviceId,
                    generatedAt = System.currentTimeMillis(),
                    parcels = SyncPushParcelRequest(items = dtos)
                )
                val response = PokkeApiClient.service.syncPush(body = request)
                if (response.isSuccessful) {
                    val acceptedCount = response.body()?.accepted?.parcels ?: unsyncedParcels.size
                    parcelRepository.updateSyncedAt(unsyncedParcels.map { it.id })
                    val now = System.currentTimeMillis()
                    syncPrefs.edit().putLong("lastParcelSyncAt", now).apply()
                    _uiState.value = _uiState.value.copy(
                        isSyncingParcel = false,
                        lastParcelSyncAt = now,
                        snackbarMessage = "荷物データを${acceptedCount}件同期しました"
                    )
                } else {
                    val code = response.code()
                    val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    appendError("syncParcels", "HTTP $code\nurl=${response.raw().request.url}\nbody=${errBody ?: "(empty)"}")
                    _uiState.value = _uiState.value.copy(
                        isSyncingParcel = false,
                        snackbarMessage = "荷物同期失敗: HTTP $code"
                    )
                }
            } catch (e: Exception) {
                appendError("syncParcels", "${e::class.simpleName}: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSyncingParcel = false,
                    snackbarMessage = "荷物同期失敗: ${e.message}"
                )
            }
        }
    }

    fun uploadAllParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAllParcels = true, snackbarMessage = null)
            try {
                val allParcels = parcelRepository.getAllParcels()
                if (allParcels.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isUploadingAllParcels = false,
                        snackbarMessage = "荷物データがありません"
                    )
                    return@launch
                }

                val deviceId = syncPrefs.getString("deviceId", null) ?: run {
                    val id = "pokke-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                    syncPrefs.edit().putString("deviceId", id).apply()
                    id
                }
                val now = System.currentTimeMillis()
                val dtos = allParcels.map { it.toSyncDto().copy(updatedAt = now) }
                val request = SyncPushRequest(
                    deviceId = deviceId,
                    generatedAt = now,
                    parcels = SyncPushParcelRequest(items = dtos)
                )
                val response = PokkeApiClient.service.syncPush(body = request)
                if (response.isSuccessful) {
                    val acceptedCount = response.body()?.accepted?.parcels ?: allParcels.size
                    parcelRepository.updateSyncedAt(allParcels.map { it.id })
                    syncPrefs.edit().putLong("lastParcelSyncAt", now).apply()
                    _uiState.value = _uiState.value.copy(
                        isUploadingAllParcels = false,
                        lastParcelSyncAt = now,
                        snackbarMessage = "全荷物データを${acceptedCount}件アップロードしました"
                    )
                } else {
                    val code = response.code()
                    val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    appendError("uploadAllParcels", "HTTP $code\nurl=${response.raw().request.url}\nbody=${errBody ?: "(empty)"}")
                    _uiState.value = _uiState.value.copy(
                        isUploadingAllParcels = false,
                        snackbarMessage = "荷物アップロード失敗: HTTP $code"
                    )
                }
            } catch (e: Exception) {
                appendError("uploadAllParcels", "${e::class.simpleName}: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isUploadingAllParcels = false,
                    snackbarMessage = "荷物アップロード失敗: ${e.message}"
                )
            }
        }
    }

    fun pullAllParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPullingAllParcels = true, snackbarMessage = null)
            try {
                val deviceId = syncPrefs.getString("deviceId", null) ?: run {
                    val id = "pokke-${android.os.Build.MODEL}-${System.currentTimeMillis()}"
                    syncPrefs.edit().putString("deviceId", id).apply()
                    id
                }
                val request = SyncPullRequest(
                    deviceId = deviceId,
                    parcels = SyncPullParcelRequest(mode = "SNAPSHOT"),
                    ryosei = SyncPullRyoseiRequest(mode = "SNAPSHOT")
                )
                val response = PokkeApiClient.service.syncPull(body = request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val now = System.currentTimeMillis()
                        val entities = body.parcels.items.map { dto ->
                            ParcelEntity(
                                id = dto.id,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt,
                                ryoseiId = dto.ryoseiId,
                                ownerBlock = dto.ownerBlock,
                                ownerRoomName = dto.ownerRoomName,
                                ownerName = dto.ownerName,
                                parcelType = dto.parcelType,
                                note = dto.note,
                                status = dto.status,
                                isLost = dto.isLost,
                                registeredByName = dto.registeredByName,
                                deliveredAt = dto.deliveredAt,
                                deliveredByName = dto.deliveredByName,
                                lastConfirmedAt = dto.lastConfirmedAt,
                                lostConfirmedAt = dto.lostConfirmedAt,
                                syncedAt = now,
                                deviceId = null
                            )
                        }
                        val merge = parcelRepository.mergeFromServer(entities)
                        syncPrefs.edit().putLong("lastParcelSyncAt", now).apply()
                        _uiState.value = _uiState.value.copy(
                            isPullingAllParcels = false,
                            lastParcelSyncAt = now,
                            snackbarMessage = "サーバー取込: 新規${merge.inserted}/更新${merge.updated}/タブレット保持${merge.keptLocal}"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isPullingAllParcels = false)
                    }
                } else {
                    val code = response.code()
                    val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    appendError("pullAllParcels", "HTTP $code\nurl=${response.raw().request.url}\nbody=${errBody ?: "(empty)"}")
                    _uiState.value = _uiState.value.copy(
                        isPullingAllParcels = false,
                        snackbarMessage = "荷物復旧失敗: HTTP $code"
                    )
                }
            } catch (e: Exception) {
                appendError("pullAllParcels", "${e::class.simpleName}: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isPullingAllParcels = false,
                    snackbarMessage = "荷物復旧失敗: ${e.message}"
                )
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingHealth = true)
            try {
                val response = PokkeApiClient.service.health()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isCheckingHealth = false,
                        healthStatus = HealthStatus.OK,
                        snackbarMessage = "サーバー: 正常"
                    )
                } else {
                    val code = response.code()
                    val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
                    appendError("checkHealth", "HTTP $code\nurl=${response.raw().request.url}\nbody=${errBody ?: "(empty)"}")
                    _uiState.value = _uiState.value.copy(
                        isCheckingHealth = false,
                        healthStatus = HealthStatus.ERROR,
                        snackbarMessage = "サーバー: 異常 (HTTP $code)"
                    )
                }
            } catch (e: Exception) {
                appendError("checkHealth", "${e::class.simpleName}: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isCheckingHealth = false,
                    healthStatus = HealthStatus.ERROR,
                    snackbarMessage = "サーバー: 接続不可 (${e.message})"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun clearErrorLog() {
        _uiState.value = _uiState.value.copy(errorLog = emptyList())
    }

    private fun appendError(source: String, message: String) {
        val entry = ErrorEntry(
            timestamp = System.currentTimeMillis(),
            source = source,
            message = message
        )
        _uiState.value = _uiState.value.copy(
            errorLog = _uiState.value.errorLog + entry
        )
    }
}
