package com.jewish.calendar.data

import androidx.room.*
import com.jewish.calendar.model.*

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycle_records ORDER BY startDate DESC")
    suspend fun getAllCycles(): List<CycleRecord>

    @Query("SELECT * FROM cycle_records WHERE endDate IS NULL LIMIT 1")
    suspend fun getActiveCycle(): CycleRecord?

    @Query("SELECT * FROM cycle_records ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestCycle(): CycleRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleRecord): Long

    @Update
    suspend fun updateCycle(cycle: CycleRecord)

    @Delete
    suspend fun deleteCycle(cycle: CycleRecord)
}

@Dao
interface CleanDayDao {
    @Query("SELECT * FROM clean_day_checks WHERE cycleId = :cycleId ORDER BY dayNumber ASC")
    suspend fun getChecksForCycle(cycleId: Long): List<CleanDayCheck>

    @Query("SELECT * FROM clean_day_checks WHERE checkDate = :date LIMIT 1")
    suspend fun getCheckForDate(date: Long): CleanDayCheck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: CleanDayCheck): Long

    @Update
    suspend fun updateCheck(check: CleanDayCheck)
}

@Dao
interface TevilahDao {
    @Query("SELECT * FROM tevilah_records ORDER BY date DESC")
    suspend fun getAllTevilahs(): List<TevilahRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTevilah(tevilah: TevilahRecord): Long

    @Delete
    suspend fun deleteTevilah(tevilah: TevilahRecord)
}

@Database(
    entities = [CycleRecord::class, CleanDayCheck::class, TevilahRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun cleanDayDao(): CleanDayDao
    abstract fun tevilahDao(): TevilahDao
}
