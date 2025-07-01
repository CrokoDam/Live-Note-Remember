package com.crokodam.ghajinicrokoedition

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationHelper {

    /**
     * Schedules a one-time alarm after [intervalMinutes].
     */
    fun scheduleAlarm(context: Context, intervalMinutes: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )

        val triggerAtMillis = System.currentTimeMillis() + intervalMinutes * 60 * 1000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Schedules a repeating alarm every [intervalMinutes].
     * Not recommended on Android 6.0+ for battery reasons — use scheduleAlarm instead.
     */
    fun scheduleRepeatingNotification(context: Context, intervalMinutes: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )

        val intervalMillis = intervalMinutes * 60 * 1000

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }
}
