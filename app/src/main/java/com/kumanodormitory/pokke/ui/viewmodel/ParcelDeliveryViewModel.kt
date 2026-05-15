package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelStatus
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
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
import java.util.UUID

data class ParcelDeliveryUiState(
    val blocks: List<String> = emptyList(),
    val rooms: List<String> = emptyList(),
    val ryoseiWithParcels: List<RyoseiEntity> = emptyList(),
    val selectedBlock: String? = null,
    val selectedRoom: String? = null,
    val selectedRyosei: RyoseiEntity? = null,
    val parcelsForRyosei: List<ParcelEntity> = emptyList(),
    val selectedParcelIds: Set<String> = emptySet(),
    val showIdentityDialog: Boolean = false,
    val showDeliveryDialog: Boolean = false,
    val dutyPersonName: String = "",
    val isLoading: Boolean = false,
    val isDelivering: Boolean = false
)

class ParcelDeliveryViewModel(
    private val ryoseiRepository: RyoseiRepository,
    private val parcelRepository: ParcelRepository,
    private val dutyPersonRepository: DutyPersonRepository
) : ViewModel() {

    companion object {
        const val BLOCK_OTHER = "その他"
        private val ALPHANUMERIC_ONLY = Regex("^[A-Za-z0-9]+$")
    }

    private val _uiState = MutableStateFlow(ParcelDeliveryUiState())
    val uiState: StateFlow<ParcelDeliveryUiState> = _uiState.asStateFlow()

    private var allRyosei: List<RyoseiEntity> = emptyList()
    private var activeParcels: List<ParcelEntity> = emptyList()

    init {
        loadData()
        loadDutyPerson()
    }

    private fun loadDutyPerson() {
        viewModelScope.launch {
            dutyPersonRepository.getCurrentDutyPerson().collect { dutyPerson ->
                _uiState.value = _uiState.value.copy(
                    dutyPersonName = dutyPerson?.name ?: ""
                )
            }
        }
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

                val ryoseiIdsWithParcels = parcels.map { it.ryoseiId }.toSet()
                val ryoseiWithParcels = ryosei.filter { it.id in ryoseiIdsWithParcels }
                val blocks = ryosei.map { it.block }.distinct().sorted()
                val hasOther = ryosei.any { !ALPHANUMERIC_ONLY.matches(it.room) }
                val blocksWithOther = if (hasOther) blocks + BLOCK_OTHER else blocks

                _uiState.value = _uiState.value.copy(
                    blocks = blocksWithOther,
                    isLoading = false
                )

                // Re-apply current selection filters or show all
                reapplyFilters()
            }
        }
    }

    private fun reapplyFilters() {
        val state = _uiState.value
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter { it.id in ryoseiIdsWithParcels }

        val filteredByBlock = state.selectedBlock?.let { block ->
            if (block == BLOCK_OTHER) {
                ryoseiWithParcels.filter { !ALPHANUMERIC_ONLY.matches(it.room) }
            } else {
                ryoseiWithParcels.filter { it.block == block }
            }
        } ?: ryoseiWithParcels

        val rooms = state.selectedBlock?.let {
            filteredByBlock.map { it.room }.distinct().sorted()
        } ?: emptyList()

        val filteredByRoom = state.selectedRoom?.let { room ->
            filteredByBlock.filter { it.room == room }
        } ?: filteredByBlock

        _uiState.value = state.copy(
            rooms = rooms,
            ryoseiWithParcels = filteredByRoom
        )
    }

    fun selectBlock(block: String) {
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter { it.id in ryoseiIdsWithParcels }
        val filteredByBlock = if (block == BLOCK_OTHER) {
            ryoseiWithParcels.filter { !ALPHANUMERIC_ONLY.matches(it.room) }
        } else {
            ryoseiWithParcels.filter { it.block == block }
        }
        val rooms = filteredByBlock.map { it.room }.distinct().sorted()

        _uiState.value = _uiState.value.copy(
            selectedBlock = block,
            selectedRoom = null,
            selectedRyosei = null,
            rooms = rooms,
            ryoseiWithParcels = filteredByBlock,
            parcelsForRyosei = emptyList(),
            selectedParcelIds = emptySet(),
            showDeliveryDialog = false
        )
    }

    fun selectRoom(room: String) {
        val ryoseiIdsWithParcels = activeParcels.map { it.ryoseiId }.toSet()
        val ryoseiWithParcels = allRyosei.filter {
            it.id in ryoseiIdsWithParcels && it.room == room
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
            showIdentityDialog = true,
            showDeliveryDialog = false
        )
    }

    fun confirmIdentity() {
        _uiState.value = _uiState.value.copy(
            showIdentityDialog = false,
            showDeliveryDialog = true
        )
    }

    fun dismissIdentityDialog() {
        _uiState.value = _uiState.value.copy(
            showIdentityDialog = false,
            selectedRyosei = null,
            parcelsForRyosei = emptyList(),
            selectedParcelIds = emptySet()
        )
    }

    fun toggleParcelSelection(parcelId: String) {
        val current = _uiState.value.selectedParcelIds
        val updated = if (parcelId in current) current - parcelId else current + parcelId
        _uiState.value = _uiState.value.copy(selectedParcelIds = updated)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showIdentityDialog = false,
            showDeliveryDialog = false,
            selectedParcelIds = emptySet()
        )
    }

    fun resetSelection() {
        _uiState.value = _uiState.value.copy(
            selectedBlock = null,
            selectedRoom = null,
            rooms = emptyList(),
            selectedRyosei = null,
            parcelsForRyosei = emptyList(),
            selectedParcelIds = emptySet(),
            showIdentityDialog = false,
            showDeliveryDialog = false
        )
        reapplyFilters()
    }

    fun deliverSelected(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.selectedParcelIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDelivering = true)

            val dutyPerson = dutyPersonRepository.getCurrentDutyPerson().first()
            val dutyPersonName = dutyPerson?.name ?: ""
            val now = System.currentTimeMillis()

            val updatedParcels = state.parcelsForRyosei
                .filter { it.id in state.selectedParcelIds }
                .map { parcel ->
                    parcel.copy(
                        status = ParcelStatus.RECEIVED.name,
                        deliveredAt = now,
                        deliveredByName = dutyPersonName,
                        updatedAt = now,
                        syncedAt = null
                    )
                }
            val logs = updatedParcels.map { parcel ->
                OperationLogEntity(
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    parcelId = parcel.id,
                    operationType = "DELIVER",
                    operatedByName = dutyPersonName,
                    metadata = null
                )
            }

            parcelRepository.deliverParcelsWithLogs(updatedParcels, logs)

            _uiState.value = _uiState.value.copy(
                isDelivering = false,
                showDeliveryDialog = false,
                selectedParcelIds = emptySet()
            )
            onComplete()
        }
    }
}
