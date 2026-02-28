package com.jewish.calendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.jewish.calendar.data.ZmanimAlarm
import com.jewish.calendar.data.ZmanimAlarmDao
import com.jewish.calendar.data.ZmanimRepository
import com.jewish.calendar.model.getZmanByKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZmanimAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmDao: ZmanimAlarmDao,
    private val zmanimRepository: ZmanimRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun scheduleAlarm(
        key: String,
        label: String,
        scheduledTimeMs: Long,
        repeatDaily: Boolean = false,
        lat: Double = 31.7683,
        lon: Double = 35.2137
    ) {
        if (scheduledTimeMs <= System.currentTimeMillis()) return

        alarmDao.insert(
            ZmanimAlarm(
                zmanimKey = key,
                label = label,
                scheduledTime = scheduledTimeMs,
                repeatDaily = repeatDaily,
                lat = lat,
                lon = lon
            )
        )

        val pi = buildPendingIntent(key, label, repeatDaily, lat, lon)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, scheduledTimeMs, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTimeMs, pi)
        }
    }

    suspend fun cancelAlarm(key: String) {
        alarmDao.delete(key)
        val pi = PendingIntent.getBroadcast(
            context, key.hashCode(),
            Intent(context, ZmanimAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    suspend fun getActiveAlarmKeys(): Set<String> =
        alarmDao.getAllActive().map { it.zmanimKey }.toSet()

    /** Called on boot: reschedule pending alarms; for daily alarms that fired while off, recalculate. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        alarmDao.getAllActive().forEach { alarm ->
            if (alarm.scheduledTime > now) {
                val pi = buildPendingIntent(alarm.zmanimKey, alarm.label, alarm.repeatDaily, alarm.lat, alarm.lon)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.scheduledTime, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.scheduledTime, pi)
                }
            } else if (alarm.repeatDaily) {
                // Alarm fired while phone was off — recalculate for tomorrow
                scheduleDailyNext(alarm)
            } else {
                alarmDao.delete(alarm.zmanimKey)
            }
        }
    }

    /** Calculates next occurrence for a daily alarm and schedules it. */
    suspend fun scheduleDailyNext(alarm: ZmanimAlarm) {
        val location = Location("").apply { latitude = alarm.lat; longitude = alarm.lon }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        val nextTime = zmanimRepository.calculateZmanim(tomorrow, location)
            .getZmanByKey(alarm.zmanimKey)?.time ?: return
        if (nextTime > System.currentTimeMillis()) {
            scheduleAlarm(alarm.zmanimKey, alarm.label, nextTime, true, alarm.lat, alarm.lon)
        }
    }

    private fun buildPendingIntent(
        key: String,
        label: String,
        repeatDaily: Boolean,
        lat: Double,
        lon: Double
    ): PendingIntent {
        val intent = Intent(context, ZmanimAlarmReceiver::class.java).apply {
            putExtra(ZmanimAlarmReceiver.EXTRA_KEY, key)
            putExtra(ZmanimAlarmReceiver.EXTRA_LABEL, label)
            putExtra(ZmanimAlarmReceiver.EXTRA_REPEAT_DAILY, repeatDaily)
            putExtra(ZmanimAlarmReceiver.EXTRA_LAT, lat)
            putExtra(ZmanimAlarmReceiver.EXTRA_LON, lon)
        }
        return PendingIntent.getBroadcast(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
