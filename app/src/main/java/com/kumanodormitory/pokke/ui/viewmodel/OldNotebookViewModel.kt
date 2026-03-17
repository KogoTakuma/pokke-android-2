package com.kumanodormitory.pokke.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.repository.ParcelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OldNotebookUiState(
    val parcels: List<ParcelEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OldNotebookViewModel(
    private val parcelRepository: ParcelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OldNotebookUiState())
    val uiState: StateFlow<OldNotebookUiState> = _uiState.asStateFlow()

    private val _startDate = MutableStateFlow(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000)
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(System.currentTimeMillis())
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    private val _selectedBlock = MutableStateFlow<String?>(null)
    val selectedBlock: StateFlow<String?> = _selectedBlock.asStateFlow()

    init {
        loadParcels()
    }

    fun setStartDate(date: Long) {
        _startDate.value = date
        loadParcels()
    }

    fun setEndDate(date: Long) {
        _endDate.value = date
        loadParcels()
    }

    fun setBlock(block: String?) {
        _selectedBlock.value = block
        loadParcels()
    }

    fun loadParcels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                parcelRepository.getParcelsByDateRangeAndBlock(
                    startDate = _startDate.value,
                    endDate = _endDate.value,
                    block = _selectedBlock.value
                ).collect { parcels ->
                    _uiState.value = OldNotebookUiState(
                        parcels = parcels,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
