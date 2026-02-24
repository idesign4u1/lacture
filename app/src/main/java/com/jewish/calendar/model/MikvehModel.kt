package com.jewish.calendar.model

import androidx.room.*
import java.util.Date

// Room entities for local encrypted storage

@Entity(tableName = "cycle_records")
data class CycleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,         // Unix timestamp
    val endDate: Long?,          // null if ongoing
    val notes: String = "",
    val flowIntensity: FlowIntensity = FlowIntensity.MEDIUM
)

enum class FlowIntensity(val label: String) {
    LIGHT("קל"),
    MEDIUM("בינוני"),
    HEAVY("כבד")
}

@Entity(tableName = "clean_day_checks")
data class CleanDayCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val checkDate: Long,           // Unix timestamp
    val dayNumber: Int,            // 1-7
    val isClean: Boolean,
    val note: String = "",
    val checkType: CheckType = CheckType.REGULAR
)

enum class CheckType(val label: String) {
    MORNING("בוקר"),
    AFTERNOON("אחה\"צ"),
    REGULAR("רגיל"),
    MOK_DACHUK("מוך דחוק")
)

@Entity(tableName = "tevilah_records")
data class TevilahRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mikvehName: String = "",
    val notes: String = "",
    val isSuccessful: Boolean = true
)

// Mikveh location (from API / local cache)
data class MikvehLocation(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val openingHours: String?,
    val hasAppointment: Boolean,
    val appointmentUrl: String?,
    val isAccessible: Boolean,
    val rating: Float?,
    val distance: Double? = null  // in KM from user
)

// Calculated cycle info
data class CycleStatus(
    val currentCycle: CycleRecord?,
    val isInPeriod: Boolean,
    val dayOfPeriod: Int?,
    val isInSevenCleanDays: Boolean,
    val cleanDayNumber: Int?,   // 1-7
    val projectedTevilahDate: Date?,
    val nextVesetDate: Date?,   // Expected next period
    val cleanDayChecks: List<CleanDayCheck> = emptyList()
)
