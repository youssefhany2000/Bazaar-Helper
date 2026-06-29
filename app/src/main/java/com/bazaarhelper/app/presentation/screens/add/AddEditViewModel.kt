package com.bazaarhelper.app.presentation.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarhelper.app.data.repository.RecordRepository
import com.bazaarhelper.app.domain.model.DailyRecord
import com.bazaarhelper.app.presentation.components.normalizeDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditUiState(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val salesInput: String = "",
    val purchasesInput: String = "",
    val notesInput: String = "",
    val salesError: String? = null,
    val purchasesError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val repository: RecordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recordId: Long = savedStateHandle.get<Long>("recordId") ?: -1L

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (recordId != -1L) {
            loadExistingRecord()
        } else {
            checkRecordForDate(LocalDate.now())
        }
    }

    private fun checkRecordForDate(date: LocalDate) {
        viewModelScope.launch {
            repository.getRecordByDateFlow(date)
                .take(1)
                .collect { record ->
                    if (record != null) {
                        _uiState.update {
                            it.copy(
                                id = record.id,
                                date = record.date,
                                salesInput = record.sales.toBigDecimal().stripTrailingZeros().toPlainString(),
                                purchasesInput = record.purchases.toBigDecimal().stripTrailingZeros().toPlainString(),
                                notesInput = record.notes,
                                isEditMode = true,
                                salesError = null,
                                purchasesError = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                id = 0,
                                salesInput = "",
                                purchasesInput = "",
                                notesInput = "",
                                isEditMode = false,
                                salesError = null,
                                purchasesError = null
                            )
                        }
                    }
                }
        }
    }

    private fun loadExistingRecord() {
        viewModelScope.launch {
            repository.getAllRecords()
                .take(1)
                .collect { records ->
                    val record = records.find { it.id == recordId }
                    record?.let { r ->
                        _uiState.update {
                            it.copy(
                                id = r.id,
                                date = r.date,
                                salesInput = r.sales.toBigDecimal().stripTrailingZeros().toPlainString(),
                                purchasesInput = r.purchases.toBigDecimal().stripTrailingZeros().toPlainString(),
                                notesInput = r.notes,
                                isEditMode = true
                            )
                        }
                    }
                }
        }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date, saveSuccess = false) }
        if (recordId == -1L) {
            checkRecordForDate(date)
        }
    }

    fun onSalesChange(value: String) {
        val normalized = value.normalizeDigits()
        if (normalized.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(salesInput = normalized, salesError = null, saveSuccess = false) }
        }
    }

    fun onPurchasesChange(value: String) {
        val normalized = value.normalizeDigits()
        if (normalized.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(purchasesInput = normalized, purchasesError = null, saveSuccess = false) }
        }
    }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notesInput = value, saveSuccess = false) }

    fun save() {
        val state = _uiState.value
        val sales = state.salesInput.toDoubleOrNull()
        val purchases = state.purchasesInput.toDoubleOrNull()

        var hasError = false
        if (sales == null || sales < 0) {
            _uiState.update { it.copy(salesError = "أدخل قيمة صحيحة (≥ 0)") }
            hasError = true
        }
        if (purchases == null || purchases < 0) {
            _uiState.update { it.copy(purchasesError = "أدخل قيمة صحيحة (≥ 0)") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val record = DailyRecord(
                id = state.id,
                date = state.date,
                sales = sales!!,
                purchases = purchases!!,
                notes = state.notesInput
            )
            val result = if (state.isEditMode) repository.updateRecord(record)
            else repository.saveRecord(record)

            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, saveSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isSaving = false, saveError = e.message) } }
            )
        }
    }

    fun clearSuccess() = _uiState.update { it.copy(saveSuccess = false) }
}
