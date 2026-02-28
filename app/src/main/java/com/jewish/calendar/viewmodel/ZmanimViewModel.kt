package com.jewish.calendar.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.ZmanimRepository
import com.jewish.calendar.model.ZmanimModel
import com.jewish.calendar.notifications.ZmanimAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ZmanimUiState(
    val zmanim: ZmanimModel? = null,
    val isLoading: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val error: String? = null,
    val activeAlarmKeys: Set<String> = emptySet(),
    val alarmMessage: String? = null
)

@HiltViewModel
class ZmanimViewModel @Inject constructor(
    private val zmanimRepository: ZmanimRepository,
    private val alarmScheduler: ZmanimAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZmanimUiState())
    val uiState: StateFlow<ZmanimUiState> = _uiState.asStateFlow()

    init {
        loadDefaultZmanim()
        loadActiveAlarms()
    }

    private fun loadDefaultZmanim() {
        viewModelScope.launch {
            val zmanim = zmanimRepository.getJerusalemZmanim(Date())
            _uiState.update { it.copy(zmanim = zmanim, isLoading = false) }
        }
    }

    private fun loadActiveAlarms() {
        viewModelScope.launch {
            val keys = alarmScheduler.getActiveAlarmKeys()
            _uiState.update { it.copy(activeAlarmKeys = keys) }
        }
    }

    fun onLocationGranted(location: Location, cityName: String = "מיקומי הנוכחי") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasLocationPermission = true) }
            try {
                val zmanim = zmanimRepository.calculateZmanim(Date(), location, cityName)
                _uiState.update { it.copy(zmanim = zmanim, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "לא ניתן לחשב זמנים", isLoading = false) }
            }
        }
    }

    fun onLocationDenied() {
        _uiState.update { it.copy(hasLocationPermission = false) }
    }

    fun refreshZmanim(location: Location? = null, cityName: String = "מיקומי הנוכחי") {
        if (location != null) onLocationGranted(location, cityName)
        else loadDefaultZmanim()
    }

    /** Toggle alarm for a given zman. Returns immediately; result reflected via activeAlarmKeys. */
    fun toggleAlarm(key: String, label: String, scheduledTimeMs: Long?) {
        viewModelScope.launch {
            val isActive = key in _uiState.value.activeAlarmKeys
            if (isActive) {
                alarmScheduler.cancelAlarm(key)
                _uiState.update {
                    it.copy(
                        activeAlarmKeys = it.activeAlarmKeys - key,
                        alarmMessage = "ההתרעה עבור $label בוטלה"
                    )
                }
            } else {
                if (scheduledTimeMs == null || scheduledTimeMs <= System.currentTimeMillis()) {
                    _uiState.update { it.copy(alarmMessage = "הזמן $label כבר עבר היום") }
                    return@launch
                }
                alarmScheduler.scheduleAlarm(key, label, scheduledTimeMs)
                _uiState.update {
                    it.copy(
                        activeAlarmKeys = it.activeAlarmKeys + key,
                        alarmMessage = "התרעה הוגדרה עבור $label"
                    )
                }
            }
        }
    }

    fun clearAlarmMessage() {
        _uiState.update { it.copy(alarmMessage = null) }
    }
}
