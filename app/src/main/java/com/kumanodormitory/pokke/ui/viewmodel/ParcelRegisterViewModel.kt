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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ParcelRegisterViewModel(
    private val parcelRepository: ParcelRepository,
    private val ryoseiRepository: RyoseiRepository,
    private val dutyPersonRepository: DutyPersonRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRyosei = MutableStateFlow<RyoseiEntity?>(null)
    val selectedRyosei: StateFlow<RyoseiEntity?> = _selectedRyosei.asStateFlow()

    private val _showTypeDialog = MutableStateFlow(false)
    val showTypeDialog: StateFlow<Boolean> = _showTypeDialog.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _uiState = MutableStateFlow<ParcelRegisterUiState>(ParcelRegisterUiState.Idle)
    val uiState: StateFlow<ParcelRegisterUiState> = _uiState.asStateFlow()

    init {
        loadBlocks()
    }

    private fun loadBlocks() {
        viewModelScope.launch {
            _blocks.value = ryoseiRepository.getAllBlocks().first()
        }
    }

    fun selectBlock(block: String) {
        _selectedBlock.value = block
        _selectedRoom.value = null
        _ryoseiList.value = emptyList()
        _searchQuery.value = ""
        viewModelScope.launch {
            _rooms.value = ryoseiRepository.getRoomsByBlock(block).first()
        }
    }

    fun selectRoom(room: String) {
        _selectedRoom.value = room
        _searchQuery.value = ""
        viewModelScope.launch {
            _ryoseiList.value = ryoseiRepository.getByRoom(room).first()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            val room = _selectedRoom.value
            if (room != null) {
                viewModelScope.launch {
                    _ryoseiList.value = ryoseiRepository.getByRoom(room).first()
                }
            }
            return
        }
        viewModelScope.launch {
            _ryoseiList.value = ryoseiRepository.search(query).first()
        }
    }

    fun selectRyosei(ryosei: RyoseiEntity) {
        _selectedRyosei.value = ryosei
        _note.value = ""
        _showTypeDialog.value = true
    }

    fun dismissTypeDialog() {
        _showTypeDialog.value = false
        _selectedRyosei.value = null
        _note.value = ""
    }

    fun updateNote(value: String) {
        if (value.length <= 200) {
            _note.value = value
        }
    }

    fun registerParcel(type: String, note: String) {
        val ryosei = _selectedRyosei.value ?: return
        viewModelScope.launch {
            _uiState.value = ParcelRegisterUiState.Loading
            try {
                val dutyPerson = dutyPersonRepository.getCurrentDutyPerson().first()
                val now = System.currentTimeMillis()
                val parcel = ParcelEntity(
                    id = "",  // Repository側でUUID生成
                    createdAt = now,
                    updatedAt = now,
                    ryoseiId = ryosei.id,
                    ownerBlock = ryosei.block,
                    ownerRoomName = ryosei.room,
                    ownerName = ryosei.name,
                    parcelType = type,
                    note = if (note.isBlank()) null else note,
                    status = "REGISTERED",
                    isLost = false,
                    registeredByName = dutyPerson?.name ?: ""
                )
                val parcelId = parcelRepository.registerParcel(parcel)
                operationLogRepository.addLog(
                    type = "REGISTER",
                    parcelId = parcelId,
                    operatedByName = dutyPerson?.name,
                    metadata = """{"ryoseiId":"${ryosei.id}","parcelType":"$type","ownerName":"${ryosei.name}","ownerRoom":"${ryosei.room}"}"""
                )
                _showTypeDialog.value = false
                _selectedRyosei.value = null
                _note.value = ""
                _uiState.value = ParcelRegisterUiState.Success
            } catch (e: Exception) {
                _uiState.value = ParcelRegisterUiState.Error(e.message ?: "登録に失敗しました")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ParcelRegisterUiState.Idle
    }
}

sealed interface ParcelRegisterUiState {
    data object Idle : ParcelRegisterUiState
    data object Loading : ParcelRegisterUiState
    data object Success : ParcelRegisterUiState
    data class Error(val message: String) : ParcelRegisterUiState
}
