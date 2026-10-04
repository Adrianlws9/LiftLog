package com.adrian.liftlog.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.adrian.liftlog.MainActivity

/**
 * Fires the break-timer's vibration and "Break over!" notification.
 * Registered statically in the manifest so it works even if the app
 * process (and BreakTimerService) has been killed by the OS in the
 * meantime — AlarmManager will restart just enough of the app to
 * deliver this broadcast.
 */
class BreakTimerReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "break_timer_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("BreakTimerReceiver", "onReceive fired!")
        vibrate(context)
        showFinishedNotification(context)
        BreakTimerService.remainingSeconds.postValue(null)
        BreakTimerService.totalSeconds.postValue(null)
    }

    private fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            android.util.Log.d("BreakTimerReceiver", "hasVibrator=${vibrator.hasVibrator()}")
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            android.util.Log.d("BreakTimerReceiver", "vibrate() called successfully")
        } catch (e: Exception) {
            android.util.Log.e("BreakTimerReceiver", "vibrate failed", e)
        }
    }

    private fun showFinishedNotification(context: Context) {
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Break over!")
            .setContentText("Time to get back to it.")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}