package com.jewish.calendar.data

import com.jewish.calendar.model.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MikvehRepository @Inject constructor(
    private val cycleDao: CycleDao,
    private val cleanDayDao: CleanDayDao,
    private val tevilahDao: TevilahDao
) {

    suspend fun startNewCycle(startDate: Date): Long {
        // Close any active cycle first
        cycleDao.getActiveCycle()?.let { active ->
            cycleDao.updateCycle(active.copy(endDate = startDate.time))
        }
        val newCycle = CycleRecord(startDate = startDate.time)
        return cycleDao.insertCycle(newCycle)
    }

    suspend fun endCurrentCycle(endDate: Date) {
        cycleDao.getActiveCycle()?.let { active ->
            cycleDao.updateCycle(active.copy(endDate = endDate.time))
        }
    }

    suspend fun getCycleStatus(): CycleStatus {
        val today = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val latestCycle = cycleDao.getLatestCycle()
        val activeCycle = cycleDao.getActiveCycle()

        if (latestCycle == null) {
            return CycleStatus(
                currentCycle = null,
                isInPeriod = false,
                dayOfPeriod = null,
                isInSevenCleanDays = false,
                cleanDayNumber = null,
                projectedTevilahDate = null,
                nextVesetDate = null
            )
        }

        val startDate = Calendar.getInstance().apply { timeInMillis = latestCycle.startDate }
        val endDate = latestCycle.endDate?.let { Calendar.getInstance().apply { timeInMillis = it } }

        val isInPeriod = endDate == null && daysBetween(startDate, today) < 14

        // Seven clean days start after period ends (at least 5 days from start)
        val cleanDaysStart = endDate?.let { end ->
            Calendar.getInstance().apply {
                timeInMillis = end.timeInMillis
                add(Calendar.DAY_OF_MONTH, 1) // Day after period ends
            }
        }

        val dayOfPeriod = if (isInPeriod) daysBetween(startDate, today) + 1 else null

        val cleanDayNumber = cleanDaysStart?.let { cleanStart ->
            val dayNum = daysBetween(cleanStart, today) + 1
            if (dayNum in 1..7) dayNum else null
        }

        val isInSevenCleanDays = cleanDayNumber != null

        // Tevilah is on night of day 7 of clean days
        val projectedTevilahDate = cleanDaysStart?.let { cleanStart ->
            Calendar.getInstance().apply {
                timeInMillis = cleanStart.timeInMillis
                add(Calendar.DAY_OF_MONTH, 6) // Day 7 of clean days (0-indexed)
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
            }.time
        }

        // Estimate next veset: average cycle is 28-30 days
        val nextVesetDate = Calendar.getInstance().apply {
            timeInMillis = latestCycle.startDate
            add(Calendar.DAY_OF_MONTH, 30) // Using 30-day average
        }.time

        val checks = cleanDaysStart?.let {
            cleanDayDao.getChecksForCycle(latestCycle.id)
        } ?: emptyList()

        return CycleStatus(
            currentCycle = latestCycle,
            isInPeriod = isInPeriod,
            dayOfPeriod = dayOfPeriod,
            isInSevenCleanDays = isInSevenCleanDays,
            cleanDayNumber = cleanDayNumber,
            projectedTevilahDate = projectedTevilahDate,
            nextVesetDate = nextVesetDate,
            cleanDayChecks = checks
        )
    }

    suspend fun addDailyCheck(cycleId: Long, dayNumber: Int, isClean: Boolean, note: String): Long {
        val check = CleanDayCheck(
            cycleId = cycleId,
            checkDate = System.currentTimeMillis(),
            dayNumber = dayNumber,
            isClean = isClean,
            note = note
        )
        return cleanDayDao.insertCheck(check)
    }

    suspend fun recordTevilah(date: Date, mikvehName: String): Long {
        val record = TevilahRecord(
            date = date.time,
            mikvehName = mikvehName
        )
        return tevilahDao.insertTevilah(record)
    }

    suspend fun getTevilahHistory(): List<TevilahRecord> = tevilahDao.getAllTevilahs()

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val startMillis = start.timeInMillis
        val endMillis = end.timeInMillis
        return ((endMillis - startMillis) / (1000 * 60 * 60 * 24)).toInt()
    }

    // Nearby mikveh locations (would connect to a real API/database)
    fun getNearbyMikvaot(lat: Double, lng: Double): List<MikvehLocation> {
        // Placeholder data - in production connect to Google Places API
        return listOf(
            MikvehLocation(
                id = "1",
                name = "מקווה המרכזית ירושלים",
                address = "רחוב יפו 22, ירושלים",
                city = "ירושלים",
                latitude = 31.7783,
                longitude = 35.2137,
                phone = "02-6234567",
                openingHours = "א'-ה': 19:00-23:00 | ו': 15:00-17:00",
                hasAppointment = true,
                appointmentUrl = null,
                isAccessible = true,
                rating = 4.5f
            ),
            MikvehLocation(
                id = "2",
                name = "מקווה בית חנה",
                address = "שד' בן גוריון 8, תל אביב",
                city = "תל אביב",
                latitude = 32.0753,
                longitude = 34.7818,
                phone = "03-5234567",
                openingHours = "בימי א'-ה' 20:00-23:00",
                hasAppointment = false,
                appointmentUrl = null,
                isAccessible = true,
                rating = 4.2f
            )
        )
    }
}
