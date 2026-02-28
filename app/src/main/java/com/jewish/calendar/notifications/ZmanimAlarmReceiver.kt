package com.jewish.calendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ZmanimAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: return
        NotificationHelper.show(context, label)
    }

    companion object {
        const val EXTRA_LABEL = "label"
        const val EXTRA_KEY   = "key"
    }
}
