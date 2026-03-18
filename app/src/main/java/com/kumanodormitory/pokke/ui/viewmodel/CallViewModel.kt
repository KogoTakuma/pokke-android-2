package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
import com.kumanodormitory.pokke.data.remote.dto.CallRequest
import com.kumanodormitory.pokke.data.repository.RyoseiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CallUiState(
    val blocks: List<String> = emptyList(),
    val rooms: List<String> = emptyList(),
    val ryoseiList: List<RyoseiEntity> = emptyList(),
    val selectedBlock: String? = null,
    val selectedRoom: String? = null,
    val searchQuery: String = "",
    val selectedRyosei: RyoseiEntity? = null,
    val showCallTypeDialog: Boolean = false,
    val isSending: Boolean = false,
    val snackbarMessage: String? = null
)

class CallViewModel(
    private val ryoseiRepository: RyoseiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        loadBlocks()
        loadAllRyosei()
    }

    private fun loadBlocks() {
        viewModelScope.launch {
            val blocks = ryoseiRepository.getAllBlocks().first()
            _uiState.value = _uiState.value.copy(blocks = blocks)
        }
    }

    private fun loadAllRyosei() {
        viewModelScope.launch {
            val ryosei = ryoseiRepository.getAll().first()
            _uiState.value = _uiState.value.copy(ryoseiList = ryosei)
        }
    }

    fun selectBlock(block: String) {
        _uiState.value = _uiState.value.copy(
            selectedBlock = block,
            selectedRoom = null,
            searchQuery = ""
        )
        viewModelScope.launch {
            val rooms = ryoseiRepository.getRoomsByBlock(block).first()
            val ryosei = ryoseiRepository.getByBlock(block).first()
            _uiState.value = _uiState.value.copy(rooms = rooms, ryoseiList = ryosei)
        }
    }

    fun selectRoom(room: String) {
        _uiState.value = _uiState.value.copy(selectedRoom = room)
        viewModelScope.launch {
            val ryosei = ryoseiRepository.getByRoom(room).first()
            _uiState.value = _uiState.value.copy(ryoseiList = ryosei)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            val room = _uiState.value.selectedRoom
            val block = _uiState.value.selectedBlock
            viewModelScope.launch {
                val ryosei = when {
                    room != null -> ryoseiRepository.getByRoom(room).first()
                    block != null -> ryoseiRepository.getByBlock(block).first()
                    else -> ryoseiRepository.getAll().first()
                }
                _uiState.value = _uiState.value.copy(ryoseiList = ryosei)
            }
            return
        }
        viewModelScope.launch {
            val results = ryoseiRepository.search(query).first()
            _uiState.value = _uiState.value.copy(ryoseiList = results)
        }
    }

    fun selectRyosei(ryosei: RyoseiEntity) {
        _uiState.value = _uiState.value.copy(
            selectedRyosei = ryosei,
            showCallTypeDialog = true
        )
    }

    fun sendCall(callType: String) {
        val ryosei = _uiState.value.selectedRyosei ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            try {
                val response = PokkeApiClient.service.callRyosei(
                    ryoseiId = ryosei.id,
                    body = CallRequest(type = callType)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        showCallTypeDialog = false,
                        selectedRyosei = null,
                        snackbarMessage = "${ryosei.name}さんに呼び出しを送信しました"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        showCallTypeDialog = false,
                        selectedRyosei = null,
                        snackbarMessage = "送信に失敗しました: HTTP ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    showCallTypeDialog = false,
                    selectedRyosei = null,
                    snackbarMessage = "送信に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun dismissCallTypeDialog() {
        _uiState.value = _uiState.value.copy(
            showCallTypeDialog = false,
            selectedRyosei = null
        )
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
