package com.netcar.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.netcar.app.MainActivity
import com.netcar.app.R
import com.netcar.app.model.Reminder
import com.netcar.app.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object ReminderNotificationHelper {

    const val CHANNEL_ID = "netcar_reminders_channel"
    private const val PREFS_NAME = "netcar_reminder_notifications"

    fun ensureChannelCreated(context: Context, isFr: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val name = if (isFr) "Rappels d'entretien Net-CAR" else "Net-CAR Service Reminders"
                val descriptionText = if (isFr) {
                    "Alertes natives pour les échéances kilométriques et dates d'entretien"
                } else {
                    "Native alerts for odometer and date maintenance reminders"
                }
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun postReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        body: String,
        isFr: Boolean = true
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        return runCatching {
            ensureChannelCreated(context, isFr)
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "reminders")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                reminderId.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(abs(reminderId.hashCode()), notification)
            true
        }.getOrDefault(false)
    }

    fun formatReminderNotificationContent(
        reminder: Reminder,
        vehicle: Vehicle?,
        isFr: Boolean
    ): Pair<String, String> {
        val currentOdo = vehicle?.currentOdometer ?: 0
        val now = System.currentTimeMillis()
        val isOverdue = reminder.isOverdue(currentOdo, now)
        val isDueSoon = reminder.isDueSoon(currentOdo, now)
        val vehName = vehicle?.displayName ?: "Net-CAR"
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val header = when {
            isOverdue -> if (isFr) "⚠️ Rappel en retard : ${reminder.title}" else "⚠️ Overdue: ${reminder.title}"
            isDueSoon -> if (isFr) "🔔 Rappel proche : ${reminder.title}" else "🔔 Due Soon: ${reminder.title}"
            else -> if (isFr) "🔧 Rappel programmé : ${reminder.title}" else "🔧 Scheduled Reminder: ${reminder.title}"
        }

        val details = mutableListOf<String>()
        details.add(vehName)

        reminder.nextDueOdometer?.let { targetOdo ->
            val diff = targetOdo - currentOdo
            if (diff <= 0) {
                details.add(
                    if (isFr) "Dépassé de ${abs(diff)} km (échéance : $targetOdo km)"
                    else "${abs(diff)} km overdue (target: $targetOdo km)"
                )
            } else {
                details.add(
                    if (isFr) "Dans $diff km (échéance : $targetOdo km)"
                    else "In $diff km (target: $targetOdo km)"
                )
            }
        }

        reminder.nextDueDate?.let { targetDate ->
            val days = ((targetDate - now) / 86400000L).toInt()
            val formattedDate = dateFormat.format(Date(targetDate))
            if (days < 0) {
                details.add(
                    if (isFr) "En retard de ${abs(days)} j ($formattedDate)"
                    else "${abs(days)} days overdue ($formattedDate)"
                )
            } else {
                details.add(
                    if (isFr) "Échéance le $formattedDate (dans $days j)"
                    else "Due on $formattedDate (in $days days)"
                )
            }
        }

        return header to details.joinToString(" • ")
    }

    fun checkAndNotifyDueReminders(
        context: Context,
        vehicles: List<Vehicle>,
        reminders: List<Reminder>,
        isFr: Boolean,
        forceNotifyAllActive: Boolean = false
    ): Int {
        ensureChannelCreated(context, isFr)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var count = 0

        for (reminder in reminders) {
            if (reminder.isCompleted) continue
            val vehicle = vehicles.find { it.id == reminder.vehicleId } ?: vehicles.firstOrNull()
            val currentOdo = vehicle?.currentOdometer ?: 0

            // Schedule date alarm if nextDueDate is in the future
            scheduleReminderAlarm(context, reminder, vehicle, isFr)

            val isOverdue = reminder.isOverdue(currentOdo, now)
            val isDueSoon = reminder.isDueSoon(currentOdo, now)

            if (forceNotifyAllActive || isOverdue || isDueSoon) {
                val stateKey = "notif_${reminder.id}_${if (isOverdue) "overdue" else "duesoon"}"
                val lastNotified = prefs.getLong(stateKey, 0L)
                // Avoid spamming automatically within the same 12 hours unless user explicitly tapped button
                if (forceNotifyAllActive || (now - lastNotified) > 12 * 3600 * 1000L) {
                    val (title, body) = formatReminderNotificationContent(reminder, vehicle, isFr)
                    if (postReminderNotification(context, reminder.id, title, body, isFr)) {
                        prefs.edit().putLong(stateKey, now).apply()
                        count++
                    }
                }
            }
        }
        return count
    }

    fun scheduleReminderAlarm(
        context: Context,
        reminder: Reminder,
        vehicle: Vehicle?,
        isFr: Boolean
    ) {
        val dueDate = reminder.nextDueDate ?: return
        if (reminder.isCompleted) return
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val (title, body) = formatReminderNotificationContent(reminder, vehicle, isFr)

            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("title", title)
                putExtra("body", body)
                putExtra("is_fr", isFr)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                abs(reminder.id.hashCode()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = if (dueDate > System.currentTimeMillis() + 60_000L) {
                dueDate
            } else {
                // If already due or within 1 minute, trigger in 5 seconds if not yet notified
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }
}
