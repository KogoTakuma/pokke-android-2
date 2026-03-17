package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAuthenticated: Boolean = false,
    val lostParcels: List<ParcelEntity> = emptyList(),
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class AdminViewModel(
    private val parcelRepository: ParcelRepository,
    private val operationLogRepository: OperationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    companion object {
        private const val ADMIN_PASSWORD = "pokke-admin"
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

    fun showSyncSnackbar() {
        _uiState.value = _uiState.value.copy(
            snackbarMessage = "同期機能は未実装です"
        )
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
