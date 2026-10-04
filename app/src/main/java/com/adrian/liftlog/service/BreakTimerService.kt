package com.adrian.liftlog.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class BreakTimerService : Service() {

    companion object {
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        private const val CHANNEL_ID = "break_timer_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 2001

        val remainingSeconds = androidx.lifecycle.MutableLiveData<Int?>(null)
        val totalSeconds = androidx.lifecycle.MutableLiveData<Int?>(null)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var breakEndTimeMillis = 0L
    private var secondsLeft = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            val remainingMillis = breakEndTimeMillis - System.currentTimeMillis()
            secondsLeft = (remainingMillis / 1000).toInt().coerceAtLeast(0)

            if (secondsLeft > 0) {
                remainingSeconds.postValue(secondsLeft)
                updateNotification()
                handler.postDelayed(this, 1000)
            } else {
                // The real vibration/notification is guaranteed by the
                // AlarmManager+BroadcastReceiver below, independent of
                // whether this Service is still alive. This just cleans
                // up the Service's own foreground state.
                remainingSeconds.postValue(null)
                totalSeconds.postValue(null)
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val total = intent?.getIntExtra(EXTRA_TOTAL_SECONDS, 0) ?: 0
        if (total <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        secondsLeft = total
        breakEndTimeMillis = System.currentTimeMillis() + (total * 1000L)
        totalSeconds.postValue(total)
        remainingSeconds.postValue(secondsLeft)

        acquireWakeLock(total)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)

        scheduleReliableAlarm(total)

        return START_STICKY
    }

    private fun scheduleReliableAlarm(durationSeconds: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val triggerAt = android.os.SystemClock.elapsedRealtime() + durationSeconds * 1000L

        val receiverIntent = Intent(this, BreakTimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        android.util.Log.d("BreakTimerService", "canScheduleExact=$canScheduleExact, triggerAt=$triggerAt")

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent
            )
        } else {
            // Permission not granted — fall back to a best-effort inexact alarm
            // rather than silently doing nothing. Less precise, but still fires.
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun acquireWakeLock(durationSeconds: Int) {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "LiftLog:BreakTimerWakeLock"
        )
        wakeLock?.acquire((durationSeconds * 1000L) + 5000L)
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Break Timer", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeText = String.format("%d:%02d", minutes, seconds)

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, com.adrian.liftlog.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Break timer")
            .setContentText("$timeText remaining")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        releaseWakeLock()
    }
}