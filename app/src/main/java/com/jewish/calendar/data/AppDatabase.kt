package com.jewish.calendar.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jewish.calendar.model.*

// --- CalendarEvent entity & DAO ---

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,            // start-of-day Unix timestamp
    val title: String,
    val description: String = ""
)

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE date >= :startOfDay AND date < :endOfDay ORDER BY id ASC")
    suspend fun getEventsForDate(startOfDay: Long, endOfDay: Long): List<CalendarEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
}

// --- Existing DAOs ---

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

    @Delete
    suspend fun deleteCheck(check: CleanDayCheck)

    @Query("DELETE FROM clean_day_checks WHERE cycleId = :cycleId")
    suspend fun deleteChecksForCycle(cycleId: Long)
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

// --- ZmanimAlarm entity & DAO ---

@Entity(tableName = "zmanim_alarms")
data class ZmanimAlarm(
    @PrimaryKey val zmanimKey: String,  // e.g. "sunrise", "sunset"
    val label: String,                  // e.g. "הנץ החמה"
    val scheduledTime: Long,            // Unix ms
    val isActive: Boolean = true,
    val repeatDaily: Boolean = false,   // reschedule every day at the same zman
    val lat: Double = 31.7683,          // latitude for next-day recalculation
    val lon: Double = 35.2137           // longitude for next-day recalculation
)

@Dao
interface ZmanimAlarmDao {
    @Query("SELECT * FROM zmanim_alarms WHERE isActive = 1")
    suspend fun getAllActive(): List<ZmanimAlarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: ZmanimAlarm)

    @Query("DELETE FROM zmanim_alarms WHERE zmanimKey = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM zmanim_alarms WHERE zmanimKey = :key LIMIT 1")
    suspend fun getByKey(key: String): ZmanimAlarm?
}

// --- Database ---

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS calendar_events " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "date INTEGER NOT NULL, " +
            "title TEXT NOT NULL, " +
            "description TEXT NOT NULL DEFAULT '')"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS zmanim_alarms " +
            "(zmanimKey TEXT PRIMARY KEY NOT NULL, " +
            "label TEXT NOT NULL, " +
            "scheduledTime INTEGER NOT NULL, " +
            "isActive INTEGER NOT NULL DEFAULT 1)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE zmanim_alarms ADD COLUMN repeatDaily INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE zmanim_alarms ADD COLUMN lat REAL NOT NULL DEFAULT 31.7683")
        database.execSQL("ALTER TABLE zmanim_alarms ADD COLUMN lon REAL NOT NULL DEFAULT 35.2137")
    }
}

@Database(
    entities = [CycleRecord::class, CleanDayCheck::class, TevilahRecord::class,
                CalendarEvent::class, ZmanimAlarm::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun cleanDayDao(): CleanDayDao
    abstract fun tevilahDao(): TevilahDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun zmanimAlarmDao(): ZmanimAlarmDao
}
