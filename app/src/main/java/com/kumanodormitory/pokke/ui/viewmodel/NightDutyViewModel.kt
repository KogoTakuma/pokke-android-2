package com.kumanodormitory.pokke.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NightDutyUiState(
    val phase: Int = 1,
    val parcelsByBuilding: Map<String, List<ParcelEntity>> = emptyMap(),
    val checkedIdsPhase1: Set<String> = emptySet(),
    val checkedIdsPhase2: Set<String> = emptySet(),
    val lostIds: Set<String> = emptySet(),
    val selectedTab: String = "A棟",
    val allCheckedPhase1: Boolean = false,
    val allCheckedPhase2: Boolean = false,
    val isLoading: Boolean = false,
    val isCompleting: Boolean = false
)

class NightDutyViewModel(
    private val parcelRepository: ParcelRepository,
    private val dutyPersonRepository: DutyPersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NightDutyUiState())
    val uiState: StateFlow<NightDutyUiState> = _uiState.asStateFlow()

    private var allParcelIds: Set<String> = emptySet()
    private var initialLostFilled: Boolean = false

    init {
        loadParcels()
    }

    private fun loadParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            parcelRepository.getRegisteredParcels().collect { parcels ->
                val grouped = parcels.groupBy { blockToBuilding(it.ownerBlock) }
                    .mapValues { (_, list) ->
                        list.sortedWith(
                            compareBy(
                                { it.ownerRoomName },
                                { it.ownerName },
                                { it.parcelType }
                            )
                        )
                    }
                val newAllParcelIds = parcels.map { it.id }.toSet()
                allParcelIds = newAllParcelIds

                val current = _uiState.value
                val nextLostIds = if (!initialLostFilled) {
                    initialLostFilled = true
                    parcels.filter { it.isLost }.map { it.id }.toSet()
                } else {
                    current.lostIds intersect newAllParcelIds
                }
                val nextCheckedPhase1 = current.checkedIdsPhase1 intersect newAllParcelIds
                val nextCheckedPhase2 = current.checkedIdsPhase2 intersect newAllParcelIds
                val newAllCheckedPhase1 = newAllParcelIds.isNotEmpty() &&
                    (nextCheckedPhase1 + nextLostIds).containsAll(newAllParcelIds)
                val newAllCheckedPhase2 = newAllParcelIds.isNotEmpty() &&
                    nextCheckedPhase2.containsAll(newAllParcelIds)

                _uiState.value = current.copy(
                    parcelsByBuilding = grouped,
                    isLoading = false,
                    lostIds = nextLostIds,
                    checkedIdsPhase1 = nextCheckedPhase1,
                    checkedIdsPhase2 = nextCheckedPhase2,
                    allCheckedPhase1 = newAllCheckedPhase1,
                    allCheckedPhase2 = newAllCheckedPhase2
                )
            }
        }
    }

    fun selectTab(building: String) {
        _uiState.value = _uiState.value.copy(selectedTab = building)
    }

    fun toggleCheck(parcelId: String) {
        val state = _uiState.value
        if (state.phase == 1) {
            val updated = if (parcelId in state.checkedIdsPhase1) {
                state.checkedIdsPhase1 - parcelId
            } else {
                state.checkedIdsPhase1 + parcelId
            }
            val allChecked = allParcelIds.isNotEmpty() && updated.containsAll(allParcelIds)
            _uiState.value = state.copy(
                checkedIdsPhase1 = updated,
                allCheckedPhase1 = allChecked
            )
        } else {
            val updated = if (parcelId in state.checkedIdsPhase2) {
                state.checkedIdsPhase2 - parcelId
            } else {
                state.checkedIdsPhase2 + parcelId
            }
            val allChecked = allParcelIds.isNotEmpty() && updated.containsAll(allParcelIds)
            _uiState.value = state.copy(
                checkedIdsPhase2 = updated,
                allCheckedPhase2 = allChecked
            )
        }
    }

    fun toggleLost(parcelId: String) {
        val state = _uiState.value
        if (state.phase != 1) return

        val updated = if (parcelId in state.lostIds) {
            state.lostIds - parcelId
        } else {
            state.lostIds + parcelId
        }

        val newCheckedIdsPhase1 = state.checkedIdsPhase1 + parcelId
        val examined = newCheckedIdsPhase1 + updated
        val allChecked = allParcelIds.isNotEmpty() && examined.containsAll(allParcelIds)

        _uiState.value = state.copy(
            lostIds = updated,
            checkedIdsPhase1 = newCheckedIdsPhase1,
            allCheckedPhase1 = allChecked
        )
    }

    fun advanceToPhase2() {
        if (!_uiState.value.allCheckedPhase1) return
        _uiState.value = _uiState.value.copy(
            phase = 2,
            selectedTab = BUILDING_TABS.first()
        )
    }

    fun completeNightDuty(prefs: SharedPreferences?, onComplete: () -> Unit) {
        val state = _uiState.value
        if (!state.allCheckedPhase2) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)

            val dutyPerson = dutyPersonRepository.getCurrentDutyPerson().first()
            val dutyPersonName = dutyPerson?.name ?: ""
            val now = System.currentTimeMillis()

            val allParcels = state.parcelsByBuilding.values.flatten()
            val confirmedParcels = allParcels
                .filter { it.id !in state.lostIds }
                .map { it.copy(isLost = false, lastConfirmedAt = now, updatedAt = now, syncedAt = null) }
            val lostUpdates = allParcels
                .filter { it.id in state.lostIds }
                .map { it.copy(isLost = true, lostConfirmedAt = now, updatedAt = now, syncedAt = null) }
            val newlyLostIds = state.lostIds.filter { id ->
                allParcels.firstOrNull { it.id == id }?.isLost == false
            }
            val lostLogs = newlyLostIds.map { parcelId ->
                OperationLogEntity(
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    parcelId = parcelId,
                    operationType = "MARK_LOST",
                    operatedByName = dutyPersonName,
                    metadata = null
                )
            }
            val nightDutyLog = OperationLogEntity(
                id = UUID.randomUUID().toString(),
                createdAt = now,
                parcelId = null,
                operationType = "NIGHT_DUTY_CONFIRM",
                operatedByName = dutyPersonName,
                metadata = null
            )

            parcelRepository.completeNightDutyAtomic(confirmedParcels, lostUpdates, lostLogs, nightDutyLog)

            prefs?.let { clearSuspendedData(it) }

            _uiState.value = _uiState.value.copy(isCompleting = false)
            onComplete()
        }
    }

    /**
     * 中断: 現在のチェック状態をSharedPreferencesに保存
     */
    fun suspend(prefs: SharedPreferences) {
        val state = _uiState.value
        val json = JSONObject().apply {
            put("phase", state.phase)
            put("checkedIdsPhase1", JSONArray(state.checkedIdsPhase1.toList()))
            put("checkedIdsPhase2", JSONArray(state.checkedIdsPhase2.toList()))
            put("lostIds", JSONArray(state.lostIds.toList()))
            put("savedAt", System.currentTimeMillis())
        }
        prefs.edit().putString(PREF_KEY, json.toString()).apply()
    }

    /**
     * 中断データが存在するか
     */
    fun hasSuspendedData(prefs: SharedPreferences): Boolean {
        return prefs.contains(PREF_KEY)
    }

    /**
     * 中断データが古いか (savedAt から SUSPEND_EXPIRY_MS 以上経過)。
     * JSON 破損時も true を返し、上位で自動クリアさせる。
     */
    fun isSuspendedDataStale(prefs: SharedPreferences): Boolean {
        val jsonStr = prefs.getString(PREF_KEY, null) ?: return false
        val savedAt = SAVED_AT_REGEX.find(jsonStr)
            ?.groupValues?.getOrNull(1)
            ?.toLongOrNull()
            ?: return true
        return System.currentTimeMillis() - savedAt >= SUSPEND_EXPIRY_MS
    }

    /**
     * 再開: 保存されたチェック状態を復元
     */
    fun resume(prefs: SharedPreferences) {
        val jsonStr = prefs.getString(PREF_KEY, null) ?: return
        try {
            val json = JSONObject(jsonStr)
            val phase = json.getInt("phase")
            val checked1 = jsonArrayToStringSet(json.getJSONArray("checkedIdsPhase1"))
            val checked2 = jsonArrayToStringSet(json.getJSONArray("checkedIdsPhase2"))
            val lost = jsonArrayToStringSet(json.getJSONArray("lostIds"))

            val allChecked1 = allParcelIds.isNotEmpty() && checked1.containsAll(allParcelIds)
            val allChecked2 = allParcelIds.isNotEmpty() && checked2.containsAll(allParcelIds)

            initialLostFilled = true
            _uiState.value = _uiState.value.copy(
                phase = phase,
                checkedIdsPhase1 = checked1,
                checkedIdsPhase2 = checked2,
                lostIds = lost,
                allCheckedPhase1 = allChecked1,
                allCheckedPhase2 = allChecked2
            )
        } catch (_: Exception) {
            // 破損データは無視
        }
    }

    /**
     * 中断データを削除し、ViewModelの状態を初期化
     */
    fun clearSuspendedData(prefs: SharedPreferences) {
        prefs.edit().remove(PREF_KEY).apply()
        reset()
    }

    /**
     * ViewModelの状態を初期化（phase=1、チェック状態をリセット）
     */
    fun reset() {
        val currentParcels = _uiState.value.parcelsByBuilding.values.flatten()
        val preLostIds = currentParcels.filter { it.isLost }.map { it.id }.toSet()
        initialLostFilled = currentParcels.isNotEmpty()
        _uiState.value = _uiState.value.copy(
            phase = 1,
            checkedIdsPhase1 = emptySet(),
            checkedIdsPhase2 = emptySet(),
            lostIds = preLostIds,
            allCheckedPhase1 = false,
            allCheckedPhase2 = false
        )
    }

    private fun jsonArrayToStringSet(arr: JSONArray): Set<String> {
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            set.add(arr.getString(i))
        }
        return set
    }

    private fun blockToBuilding(block: String): String {
        return when {
            block.startsWith("A") -> "A棟"
            block.startsWith("B") -> "B棟"
            block.startsWith("C") -> "C棟"
            else -> "臨キャパ"
        }
    }

    companion object {
        val BUILDING_TABS = listOf("A棟", "B棟", "C棟", "臨キャパ")
        private const val PREF_KEY = "night_duty_suspended"
        const val SUSPEND_EXPIRY_MS = 5L * 60 * 60 * 1000
        private val SAVED_AT_REGEX = Regex("\"savedAt\"\\s*:\\s*(\\d+)")
    }
}
