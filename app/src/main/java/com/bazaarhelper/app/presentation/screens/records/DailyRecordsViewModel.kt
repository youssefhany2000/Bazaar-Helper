package com.bazaarhelper.app.presentation.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarhelper.app.data.repository.RecordRepository
import com.bazaarhelper.app.domain.model.DailyRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DailyRecordsUiState(
    val allRecords: List<DailyRecord> = emptyList(),
    val filteredRecords: List<DailyRecord> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class DailyRecordsViewModel @Inject constructor(
    private val repository: RecordRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _deleteSuccess = MutableStateFlow(false)

    val uiState: StateFlow<DailyRecordsUiState> = combine(
        repository.getAllRecords(),
        _searchQuery,
        _deleteSuccess
    ) { records, query, deleted ->
        val sortedRecords = records.sortedByDescending { it.date }
        val filtered = if (query.isBlank()) {
            sortedRecords
        } else {
            sortedRecords.filter {
                it.date.toString().contains(query) || it.formatForSearch().contains(query)
            }
        }
        DailyRecordsUiState(
            allRecords = sortedRecords,
            filteredRecords = filtered,
            searchQuery = query,
            isLoading = false,
            deleteSuccess = deleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyRecordsUiState()
    )

    fun onSearchChange(query: String) = _searchQuery.update { query }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id).onSuccess {
                _deleteSuccess.update { true }
            }
        }
    }

    fun clearDeleteSuccess() = _deleteSuccess.update { false }
}

private fun DailyRecord.formatForSearch(): String =
    "${date.dayOfMonth}/${date.monthValue}/${date.year}"
