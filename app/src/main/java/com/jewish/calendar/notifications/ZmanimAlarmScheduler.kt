package com.jewish.calendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jewish.calendar.data.ZmanimAlarm
import com.jewish.calendar.data.ZmanimAlarmDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZmanimAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmDao: ZmanimAlarmDao
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun scheduleAlarm(key: String, label: String, scheduledTimeMs: Long) {
        if (scheduledTimeMs <= System.currentTimeMillis()) return

        alarmDao.insert(ZmanimAlarm(zmanimKey = key, label = label, scheduledTime = scheduledTimeMs))

        val pi = buildPendingIntent(key, label)
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

    /** Called on boot to reschedule alarms that haven't fired yet. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        alarmDao.getAllActive().forEach { alarm ->
            if (alarm.scheduledTime > now) {
                val pi = buildPendingIntent(alarm.zmanimKey, alarm.label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.scheduledTime, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.scheduledTime, pi)
                }
            } else {
                alarmDao.delete(alarm.zmanimKey)
            }
        }
    }

    private fun buildPendingIntent(key: String, label: String): PendingIntent {
        val intent = Intent(context, ZmanimAlarmReceiver::class.java).apply {
            putExtra(ZmanimAlarmReceiver.EXTRA_KEY, key)
            putExtra(ZmanimAlarmReceiver.EXTRA_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
