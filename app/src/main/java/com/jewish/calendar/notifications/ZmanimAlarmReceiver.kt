package com.jewish.calendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jewish.calendar.data.ZmanimAlarm
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ZmanimAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: ZmanimAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val repeatDaily = intent.getBooleanExtra(EXTRA_REPEAT_DAILY, false)
        val lat = intent.getDoubleExtra(EXTRA_LAT, 31.7683)
        val lon = intent.getDoubleExtra(EXTRA_LON, 35.2137)

        NotificationHelper.show(context, label)

        if (repeatDaily) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Schedule tomorrow's occurrence of this daily alarm
                    alarmScheduler.scheduleDailyNext(
                        ZmanimAlarm(
                            zmanimKey = key,
                            label = label,
                            scheduledTime = 0L, // irrelevant — scheduleDailyNext calculates it
                            repeatDaily = true,
                            lat = lat,
                            lon = lon
                        )
                    )
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_LABEL        = "label"
        const val EXTRA_KEY          = "key"
        const val EXTRA_REPEAT_DAILY = "repeatDaily"
        const val EXTRA_LAT          = "lat"
        const val EXTRA_LON          = "lon"
    }
}
