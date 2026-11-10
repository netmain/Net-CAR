package com.netcar.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val reminderId = intent.getStringExtra("reminder_id") ?: "reminder_alarm"
            val title = intent.getStringExtra("title") ?: "Rappel d'entretien Net-CAR"
            val body = intent.getStringExtra("body") ?: "Un entretien programmé arrive à échéance."
            val isFr = intent.getBooleanExtra("is_fr", true)

            ReminderNotificationHelper.postReminderNotification(
                context = context,
                reminderId = reminderId,
                title = title,
                body = body,
                isFr = isFr
            )
        }
    }
}
