package com.kumanodormitory.pokke.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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
    private val operationLogRepository: OperationLogRepository,
    private val dutyPersonRepository: DutyPersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NightDutyUiState())
    val uiState: StateFlow<NightDutyUiState> = _uiState.asStateFlow()

    private var allParcelIds: Set<String> = emptySet()

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
                allParcelIds = parcels.map { it.id }.toSet()

                _uiState.value = _uiState.value.copy(
                    parcelsByBuilding = grouped,
                    isLoading = false,
                    allCheckedPhase1 = false,
                    allCheckedPhase2 = false
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

        // Lost items are also considered "checked" for phase1 completion
        val checkedWithLost = state.checkedIdsPhase1 + updated
        val allChecked = allParcelIds.isNotEmpty() && checkedWithLost.containsAll(allParcelIds)

        _uiState.value = state.copy(
            lostIds = updated,
            checkedIdsPhase1 = state.checkedIdsPhase1 + parcelId,
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

            // Update lastConfirmedAt for all parcels (batch)
            parcelRepository.confirmNightDuty(allParcelIds.toList())

            // Mark lost parcels
            for (parcelId in state.lostIds) {
                parcelRepository.markLost(parcelId)
                operationLogRepository.addLog("MARK_LOST", parcelId, dutyPersonName, null)
            }

            // Log night duty confirmation
            operationLogRepository.addLog("NIGHT_DUTY_CONFIRM", null, dutyPersonName, null)

            // Clear suspended data on completion
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
     * 中断データを削除
     */
    fun clearSuspendedData(prefs: SharedPreferences) {
        prefs.edit().remove(PREF_KEY).apply()
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
    }
}
