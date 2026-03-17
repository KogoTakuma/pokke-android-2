package com.kumanodormitory.pokke.ui.viewmodel

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

    companion object {
        val BUILDING_TABS = listOf("A棟", "B棟", "C棟", "臨キャパ")
    }

    init {
        loadParcels()
    }

    private fun loadParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            parcelRepository.getRegisteredParcels().collect { parcels ->
                val grouped = parcels.groupBy { blockToBuilding(it.ownerBlock) }
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

    fun completeNightDuty(onComplete: () -> Unit) {
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

            _uiState.value = _uiState.value.copy(isCompleting = false)
            onComplete()
        }
    }

    private fun blockToBuilding(block: String): String {
        return when {
            block.startsWith("A") -> "A棟"
            block.startsWith("B") -> "B棟"
            block.startsWith("C") -> "C棟"
            else -> "臨キャパ"
        }
    }
}
