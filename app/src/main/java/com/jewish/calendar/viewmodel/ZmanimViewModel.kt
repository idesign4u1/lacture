package com.jewish.calendar.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.ZmanimRepository
import com.jewish.calendar.model.ZmanimModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ZmanimUiState(
    val zmanim: ZmanimModel? = null,
    val isLoading: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ZmanimViewModel @Inject constructor(
    private val zmanimRepository: ZmanimRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZmanimUiState())
    val uiState: StateFlow<ZmanimUiState> = _uiState.asStateFlow()

    init {
        loadDefaultZmanim()
    }

    private fun loadDefaultZmanim() {
        viewModelScope.launch {
            val zmanim = zmanimRepository.getJerusalemZmanim(Date())
            _uiState.update { it.copy(zmanim = zmanim, isLoading = false) }
        }
    }

    fun onLocationGranted(location: Location) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasLocationPermission = true) }
            try {
                val zmanim = zmanimRepository.calculateZmanim(Date(), location)
                _uiState.update { it.copy(zmanim = zmanim, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "לא ניתן לחשב זמנים", isLoading = false)
                }
            }
        }
    }

    fun onLocationDenied() {
        _uiState.update { it.copy(hasLocationPermission = false) }
    }

    fun refreshZmanim(location: Location? = null) {
        if (location != null) onLocationGranted(location)
        else loadDefaultZmanim()
    }
}
