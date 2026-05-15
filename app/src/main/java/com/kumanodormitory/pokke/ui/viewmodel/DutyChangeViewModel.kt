package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DutyChangeViewModel(
    private val ryoseiRepository: RyoseiRepository,
    private val dutyPersonRepository: DutyPersonRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

    companion object {
        const val BLOCK_OTHER = "その他"
    }

    private val _blocks = MutableStateFlow<List<String>>(emptyList())
    val blocks: StateFlow<List<String>> = _blocks.asStateFlow()

    private val _rooms = MutableStateFlow<List<String>>(emptyList())
    val rooms: StateFlow<List<String>> = _rooms.asStateFlow()

    private val _ryoseiList = MutableStateFlow<List<RyoseiEntity>>(emptyList())
    val ryoseiList: StateFlow<List<RyoseiEntity>> = _ryoseiList.asStateFlow()

    private val _selectedBlock = MutableStateFlow<String?>(null)
    val selectedBlock: StateFlow<String?> = _selectedBlock.asStateFlow()

    private val _selectedRoom = MutableStateFlow<String?>(null)
    val selectedRoom: StateFlow<String?> = _selectedRoom.asStateFlow()

    private val _selectedRyosei = MutableStateFlow<RyoseiEntity?>(null)
    val selectedRyosei: StateFlow<RyoseiEntity?> = _selectedRyosei.asStateFlow()

    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    private val _uiState = MutableStateFlow<DutyChangeUiState>(DutyChangeUiState.Idle)
    val uiState: StateFlow<DutyChangeUiState> = _uiState.asStateFlow()

    init {
        loadBlocks()
        loadAllRyosei()
    }

    private fun loadBlocks() {
        viewModelScope.launch {
            ryoseiRepository.getAllBlocks().collect { blocks ->
                val hasOther = ryoseiRepository.getNonAlphanumericRooms().first().isNotEmpty()
                _blocks.value = if (hasOther) blocks + BLOCK_OTHER else blocks
            }
        }
    }

    private fun loadAllRyosei() {
        viewModelScope.launch {
            ryoseiRepository.getAll().collect { ryosei ->
                _ryoseiList.value = ryosei
            }
        }
    }

    fun selectBlock(block: String) {
        _selectedBlock.value = block
        _selectedRoom.value = null
        viewModelScope.launch {
            if (block == BLOCK_OTHER) {
                _rooms.value = ryoseiRepository.getNonAlphanumericRooms().first()
                _ryoseiList.value = ryoseiRepository.getByNonAlphanumericRoom().first()
            } else {
                _rooms.value = ryoseiRepository.getRoomsByBlock(block).first()
                _ryoseiList.value = ryoseiRepository.getByBlock(block).first()
            }
        }
    }

    fun selectRoom(room: String) {
        _selectedRoom.value = room
        viewModelScope.launch {
            _ryoseiList.value = ryoseiRepository.getByRoom(room).first()
        }
    }

    fun selectRyosei(ryosei: RyoseiEntity) {
        _selectedRyosei.value = ryosei
        _showConfirmDialog.value = true
    }

    fun dismissDialog() {
        _showConfirmDialog.value = false
        _selectedRyosei.value = null
    }

    fun resetUiState() {
        _uiState.value = DutyChangeUiState.Idle
    }

    fun confirmDutyChange() {
        val ryosei = _selectedRyosei.value ?: return
        viewModelScope.launch {
            _uiState.value = DutyChangeUiState.Loading
            try {
                val displayName = "${ryosei.room} ${ryosei.name}"
                dutyPersonRepository.changeDutyPerson(
                    DutyPersonEntity(
                        id = "duty_person",
                        name = displayName,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                operationLogRepository.addLog(
                    type = "DUTY_CHANGE",
                    parcelId = null,
                    operatedByName = displayName,
                    metadata = null
                )
                _showConfirmDialog.value = false
                _uiState.value = DutyChangeUiState.Success
            } catch (e: Exception) {
                _uiState.value = DutyChangeUiState.Error(e.message ?: "事務当番の交代に失敗しました")
            }
        }
    }
}

sealed interface DutyChangeUiState {
    data object Idle : DutyChangeUiState
    data object Loading : DutyChangeUiState
    data object Success : DutyChangeUiState
    data class Error(val message: String) : DutyChangeUiState
}
