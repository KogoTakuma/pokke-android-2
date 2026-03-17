package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.repository.DutyPersonRepository
import com.kumanodormitory.pokke.data.repository.OperationLogRepository
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentDutyPersonName: String = "",
    val recentLogs: List<OperationLogEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val operationLogRepository: OperationLogRepository,
    private val dutyPersonRepository: DutyPersonRepository,
    private val parcelRepository: ParcelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _showCancelDialog = MutableStateFlow<OperationLogEntity?>(null)
    val showCancelDialog: StateFlow<OperationLogEntity?> = _showCancelDialog.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                dutyPersonRepository.getCurrentDutyPerson(),
                operationLogRepository.getRecentLogs(50)
            ) { dutyPerson: DutyPersonEntity?, logs: List<OperationLogEntity> ->
                HomeUiState(
                    currentDutyPersonName = dutyPerson?.name ?: "",
                    recentLogs = logs,
                    isLoading = false,
                    error = null
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun isCancellable(log: OperationLogEntity): Boolean {
        val isCancellableType = log.operationType == "REGISTER" || log.operationType == "DELIVER"
        val withinTimeLimit = System.currentTimeMillis() - log.createdAt < 10 * 60 * 1000
        return isCancellableType && withinTimeLimit
    }

    fun requestCancel(log: OperationLogEntity) {
        _showCancelDialog.value = log
    }

    fun dismissCancelDialog() {
        _showCancelDialog.value = null
    }

    fun confirmCancel() {
        val log = _showCancelDialog.value ?: return
        _showCancelDialog.value = null
        cancelOperation(
            logId = log.id,
            operationType = log.operationType,
            parcelId = log.parcelId
        )
    }

    private fun cancelOperation(logId: String, operationType: String, parcelId: String?) {
        viewModelScope.launch {
            try {
                val dutyPersonName = _uiState.value.currentDutyPersonName
                when (operationType) {
                    "REGISTER" -> {
                        if (parcelId != null) {
                            parcelRepository.cancelRegister(parcelId)
                            operationLogRepository.addLog(
                                type = "CANCEL_REGISTER",
                                parcelId = parcelId,
                                operatedByName = dutyPersonName,
                                metadata = null
                            )
                        }
                    }
                    "DELIVER" -> {
                        if (parcelId != null) {
                            parcelRepository.cancelDeliver(parcelId)
                            operationLogRepository.addLog(
                                type = "CANCEL_DELIVER",
                                parcelId = parcelId,
                                operatedByName = dutyPersonName,
                                metadata = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "取消処理に失敗しました"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
