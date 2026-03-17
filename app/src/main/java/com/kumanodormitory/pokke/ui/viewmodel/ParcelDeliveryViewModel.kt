package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParcelDeliveryUiState(
    val blocks: List<String> = emptyList(),
    val rooms: List<String> = emptyList(),
    val ryoseiWithParcels: List<RyoseiEntity> = emptyList(),
    val selectedBlock: String? = null,
    val selectedRoom: String? = null,
    val selectedRyosei: RyoseiEntity? = null,
    val parcelsForRyosei: List<ParcelEntity> = emptyList(),
    val selectedParcelIds: Set<String> = emptySet(),
    val showDeliveryDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isDelivering: Boolean = false
)

class ParcelDeliveryViewModel(
    private val ryoseiRepository: RyoseiRepository,
    private val parcelRepository: ParcelRepository,
    private val dutyPersonRepository: DutyPersonRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParcelDeliveryUiState())
    val uiState: StateFlow<ParcelDeliveryUiState> = _uiState.asStateFlow()

    private var allRyosei: List<RyoseiEntity> = emptyList()
    private var activeParcels: List<ParcelEntity> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                ryoseiRepository.getRyoseiWithParcels(),
                parcelRepository.getRegisteredParcels()
            ) { ryosei, parcels ->
                Pair(ryosei, parcels)
            }.collect { (ryosei, parcels) ->
                allRyosei = ryosei
                activeParcels = parcels

                val blocks = ryosei.map { it.block }.distinct().sorted()

                _uiState.value = _uiState.value.copy(
                    blocks = blocks,
                    isLoading = false
                )

                // Re-apply current selection filters
                _uiState.value.selectedBlock?.let { reapplyFilters() }
            }
        }
    }

    private fun reapplyFilters() {
        val state = _uiState.value
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter { it.id in ryoseiIdsWithParcels }

        val filteredByBlock = state.selectedBlock?.let { block ->
            ryoseiWithParcels.filter { it.block == block }
        } ?: emptyList()

        val rooms = filteredByBlock.map { it.room }.distinct().sorted()

        val filteredByRoom = state.selectedRoom?.let { room ->
            filteredByBlock.filter { it.room == room }
        } ?: emptyList()

        _uiState.value = state.copy(
            rooms = rooms,
            ryoseiWithParcels = filteredByRoom
        )
    }

    fun selectBlock(block: String) {
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter { it.id in ryoseiIdsWithParcels }
        val filteredByBlock = ryoseiWithParcels.filter { it.block == block }
        val rooms = filteredByBlock.map { it.room }.distinct().sorted()

        _uiState.value = _uiState.value.copy(
            selectedBlock = block,
            selectedRoom = null,
            selectedRyosei = null,
            rooms = rooms,
            ryoseiWithParcels = emptyList(),
            parcelsForRyosei = emptyList(),
            selectedParcelIds = emptySet(),
            showDeliveryDialog = false
        )
    }

    fun selectRoom(room: String) {
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter {
            it.id in ryoseiIdsWithParcels && it.block == _uiState.value.selectedBlock && it.room == room
        }

        _uiState.value = _uiState.value.copy(
            selectedRoom = room,
            selectedRyosei = null,
            ryoseiWithParcels = ryoseiWithParcels,
            parcelsForRyosei = emptyList(),
            selectedParcelIds = emptySet(),
            showDeliveryDialog = false
        )
    }

    fun selectRyosei(ryosei: RyoseiEntity) {
        val parcels = activeParcels.filter { it.ryoseiId == ryosei.id }

        _uiState.value = _uiState.value.copy(
            selectedRyosei = ryosei,
            parcelsForRyosei = parcels,
            selectedParcelIds = parcels.map { it.id }.toSet(),
            showDeliveryDialog = true
        )
    }

    fun toggleParcelSelection(parcelId: String) {
        val current = _uiState.value.selectedParcelIds
        val updated = if (parcelId in current) current - parcelId else current + parcelId
        _uiState.value = _uiState.value.copy(selectedParcelIds = updated)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showDeliveryDialog = false,
            selectedParcelIds = emptySet()
        )
    }

    fun deliverSelected(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.selectedParcelIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDelivering = true)

            val dutyPerson = dutyPersonRepository.getCurrentDutyPerson().first()
            val dutyPersonName = dutyPerson?.name ?: ""

            for (parcelId in state.selectedParcelIds) {
                parcelRepository.deliverParcel(parcelId, dutyPersonName)
                operationLogRepository.addLog("DELIVER", parcelId, dutyPersonName, null)
            }

            _uiState.value = _uiState.value.copy(
                isDelivering = false,
                showDeliveryDialog = false,
                selectedParcelIds = emptySet()
            )
            onComplete()
        }
    }
}
