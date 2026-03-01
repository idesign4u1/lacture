package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.GratitudeDao
import com.jewish.calendar.data.GratitudeEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class GratitudeUiState(
    val entries: List<GratitudeEntry> = emptyList(),
    val inputText: String = "",
    val showAddDialog: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class GratitudeViewModel @Inject constructor(
    private val gratitudeDao: GratitudeDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GratitudeUiState())
    val uiState: StateFlow<GratitudeUiState> = _uiState.asStateFlow()

    init { loadEntries() }

    private fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entries = gratitudeDao.getAll()
            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun showDialog() = _uiState.update { it.copy(showAddDialog = true, inputText = "") }

    fun dismissDialog() = _uiState.update { it.copy(showAddDialog = false, inputText = "") }

    fun saveEntry() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            gratitudeDao.insert(
                GratitudeEntry(
                    dateMs  = System.currentTimeMillis(),
                    content = text
                )
            )
            _uiState.update { it.copy(showAddDialog = false, inputText = "") }
            loadEntries()
        }
    }

    fun deleteEntry(entry: GratitudeEntry) {
        viewModelScope.launch {
            gratitudeDao.delete(entry)
            loadEntries()
        }
    }

    fun formatDate(dateMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
        val day   = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year  = cal.get(Calendar.YEAR)
        return "$day/$month/$year"
    }
}
