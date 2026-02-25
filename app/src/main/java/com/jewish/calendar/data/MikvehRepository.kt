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

    suspend fun deleteCycle(cycle: CycleRecord) {
        cleanDayDao.deleteChecksForCycle(cycle.id)
        cycleDao.deleteCycle(cycle)
    }

    suspend fun getCycleStatus(): CycleStatus {
        val today = Calendar.getInstance()

        val latestCycle = cycleDao.getLatestCycle()

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

        val cleanDaysStart = endDate?.let { end ->
            Calendar.getInstance().apply {
                timeInMillis = end.timeInMillis
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val dayOfPeriod = if (isInPeriod) daysBetween(startDate, today) + 1 else null

        val cleanDayNumber = cleanDaysStart?.let { cleanStart ->
            val dayNum = daysBetween(cleanStart, today) + 1
            if (dayNum in 1..7) dayNum else null
        }

        val isInSevenCleanDays = cleanDayNumber != null

        val projectedTevilahDate = cleanDaysStart?.let { cleanStart ->
            Calendar.getInstance().apply {
                timeInMillis = cleanStart.timeInMillis
                add(Calendar.DAY_OF_MONTH, 6)
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
            }.time
        }

        val nextVesetDate = Calendar.getInstance().apply {
            timeInMillis = latestCycle.startDate
            add(Calendar.DAY_OF_MONTH, 30)
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

    suspend fun deleteTevilah(record: TevilahRecord) {
        tevilahDao.deleteTevilah(record)
    }

    suspend fun getTevilahHistory(): List<TevilahRecord> = tevilahDao.getAllTevilahs()

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val startMillis = start.timeInMillis
        val endMillis = end.timeInMillis
        return ((endMillis - startMillis) / (1000 * 60 * 60 * 24)).toInt()
    }
}
