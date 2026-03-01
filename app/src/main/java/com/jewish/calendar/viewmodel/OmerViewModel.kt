package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.HebrewCalendarRepository
import com.jewish.calendar.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.util.*
import javax.inject.Inject

data class OmerUiState(
    val omerDay: Int? = null,          // null = not omer season
    val omerText: String = "",
    val sefirot: String = "",
    val confirmedToday: Boolean = false,
    val streak: Int = 0,
    val showNusach: Boolean = false
)

@HiltViewModel
class OmerViewModel @Inject constructor(
    private val calendarRepository: HebrewCalendarRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OmerUiState())
    val uiState: StateFlow<OmerUiState> = _uiState.asStateFlow()

    private val sefirot = listOf(
        "חֶסֶד", "גְּבוּרָה", "תִּפְאֶרֶת", "נֶצַח", "הוֹד", "יְסוֹד", "מַלְכוּת"
    )

    init { loadOmer() }

    private fun loadOmer() {
        viewModelScope.launch {
            val cal = JewishCalendar(Calendar.getInstance()).apply { inIsrael = true }
            val day = try {
                val d = cal.dayOfOmer
                if (d in 1..49) d else null
            } catch (e: Exception) { null }

            val text = if (day != null) calendarRepository.getOmerText(day) else ""
            val sefirah = if (day != null) getSefirot(day) else ""

            val todayStr = todayKey()
            val streak = prefs.getOmerStreak()
            val confirmed = prefs.getOmerLastDate() == todayStr

            _uiState.update {
                it.copy(
                    omerDay      = day,
                    omerText     = text,
                    sefirot      = sefirah,
                    confirmedToday = confirmed,
                    streak       = streak
                )
            }
        }
    }

    fun confirmCounted() {
        viewModelScope.launch {
            val todayStr = todayKey()
            val yesterday = yesterdayKey()
            val lastDate = prefs.getOmerLastDate()
            val newStreak = if (lastDate == yesterday) (prefs.getOmerStreak() + 1) else 1
            prefs.saveOmerConfirm(todayStr, newStreak)
            _uiState.update { it.copy(confirmedToday = true, streak = newStreak) }
        }
    }

    fun toggleNusach() = _uiState.update { it.copy(showNusach = !it.showNusach) }

    private fun getSefirot(day: Int): String {
        val week = (day - 1) / 7
        val d    = (day - 1) % 7
        return "${sefirot[d]} שֶׁבְּ${sefirot[week]}"
    }

    private fun todayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)+1}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun yesterdayKey(): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)+1}-${c.get(Calendar.DAY_OF_MONTH)}"
    }
}
