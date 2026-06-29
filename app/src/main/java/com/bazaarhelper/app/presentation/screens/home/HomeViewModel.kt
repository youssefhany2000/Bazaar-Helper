package com.bazaarhelper.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarhelper.app.data.repository.RecordRepository
import com.bazaarhelper.app.domain.model.DailyRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val todayRecord: DailyRecord? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RecordRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository
        .getRecordByDateFlow(LocalDate.now())
        .map { record -> HomeUiState(todayRecord = record, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
}
