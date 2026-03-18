package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.SeedData
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
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
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val healthStatus: HealthStatus = HealthStatus.UNKNOWN,
    val isCheckingHealth: Boolean = false
)

enum class HealthStatus { UNKNOWN, OK, ERROR }

class AdminViewModel(
    private val parcelRepository: ParcelRepository,
    private val ryoseiRepository: RyoseiRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
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
    }

    fun confirmLost(parcelId: String) {
        viewModelScope.launch {
            try {
                parcelRepository.markLost(parcelId)
                operationLogRepository.addLog(
                    type = "MARK_LOST",
                    parcelId = parcelId,
                    operatedByName = null,
                    metadata = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "紛失確定に失敗しました: ${e.message}"
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

    fun manualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, snackbarMessage = null)
            try {
                val response = PokkeApiClient.service.getRyosei()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val entities = body.ryosei.map { dto ->
                            RyoseiEntity(
                                id = dto.id, name = dto.name, nameKana = dto.nameKana,
                                nameAlphabet = dto.nameAlphabet, room = dto.room,
                                block = dto.block, leavingDate = dto.leavingDate,
                                discordStatus = dto.discordStatus
                            )
                        }
                        ryoseiRepository.replaceAll(entities)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            snackbarMessage = "寮生データを${entities.size}件同期しました"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "同期失敗: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "同期失敗: ${e.message}"
                )
            }
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingHealth = true)
            try {
                val response = PokkeApiClient.service.health()
                _uiState.value = _uiState.value.copy(
                    isCheckingHealth = false,
                    healthStatus = if (response.isSuccessful) HealthStatus.OK else HealthStatus.ERROR,
                    snackbarMessage = if (response.isSuccessful) "サーバー: 正常" else "サーバー: 異常 (HTTP ${response.code()})"
                )
            } catch (e: Exception) {
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
}
