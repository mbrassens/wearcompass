/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frank.wearcompass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.tiles.TileService
import com.frank.wearcompass.util.formatDisplayTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.os.SystemClock

sealed class TimerState {
    object Idle : TimerState()
    data class Running(val remainingTime: Long) : TimerState()
    data class Finished(val finalTime: Long) : TimerState()
}

class TimerService : LifecycleService() {
    private val binder = TimerBinder()
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences

    private var countDownTimer: CountDownTimer? = null
    private var ongoingActivity: OngoingActivity? = null // Make it a class member

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState = _timerState.asStateFlow()

    private val _timerInitialValue = MutableStateFlow(0L)
    val timerInitialValue = _timerInitialValue.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP_TIMER) {
            stopTimer()
            return START_NOT_STICKY
        }
        intent?.getIntExtra(EXTRA_TIMER_DURATION, 0)?.let {
            if (it > 0) {
                startTimer(it.toLong())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun startTimer(duration: Long) {
        _timerInitialValue.value = duration
        countDownTimer?.cancel()
        _timerState.value = TimerState.Running(duration)

        val notificationBuilder = buildNotification(duration)
            .setOngoing(true) // This is required for ongoing activities

        // Create the status for the ongoing activity
        val status = androidx.wear.ongoing.Status.Builder()
            .addTemplate("Timer Running")
            .build()

        Log.d("TimerService", "Creating ongoing activity with status: $status")

        ongoingActivity = OngoingActivity.Builder(
            applicationContext,
            NOTIFICATION_ID,
            notificationBuilder
        )
            .setAnimatedIcon(R.drawable.ic_timer_google_style) // Use simpler icon for active mode
            .setStaticIcon(R.drawable.ic_timer_google_style) // Use simpler icon for ambient mode
            .setTouchIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, CountdownActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setStatus(status) // Set the status text
            .build()
        
        try {
            Log.d("TimerService", "About to apply ongoing activity...")
            ongoingActivity?.apply(applicationContext)
            Log.d("TimerService", "Ongoing activity applied successfully!")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to apply ongoing activity", e)
        }

        // Start foreground service AFTER applying ongoing activity
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        Log.d("TimerService", "Started foreground service with notification ID: $NOTIFICATION_ID")

        countDownTimer = object : CountDownTimer(duration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerState.value = TimerState.Running(millisUntilFinished)
                updateNotification(millisUntilFinished)
                saveTimerState()
                requestTileUpdate()
                
                // Update the ongoing activity status
                val updatedStatus = androidx.wear.ongoing.Status.Builder()
                    .addTemplate("Timer Running")
                    .build()
                try {
                    ongoingActivity?.update(applicationContext, updatedStatus)
                    Log.d("TimerService", "Updated ongoing activity status: $updatedStatus")
                } catch (e: Exception) {
                    Log.e("TimerService", "Failed to update ongoing activity", e)
                }
            }

            override fun onFinish() {
                _timerState.value = TimerState.Finished(timerInitialValue.value)
                saveTimerState()
                updateNotification(0)
                requestTileUpdate()
                Log.d("TimerService", "Timer finished, ongoing activity should stop")
            }
        }.start()
        saveTimerState()
        requestTileUpdate()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        _timerState.value = TimerState.Idle
        ongoingActivity = null // Clear the ongoing activity
        stopForeground(true)
        saveTimerState()
        requestTileUpdate()
        Log.d("TimerService", "Timer stopped, ongoing activity cleared")
    }

    private fun updateNotification(remainingTime: Long) {
        val notification = buildNotification(remainingTime).build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(remainingTime: Long): NotificationCompat.Builder {
        val channelId = "timer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH // Changed to HIGH importance
            )
            notificationManager.createNotificationChannel(channel)
            Log.d("TimerService", "Created notification channel: $channelId with HIGH importance")
        }

        val intent = Intent(this, CountdownActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Timer")
            .setContentText("Remaining time: ${com.frank.wearcompass.util.formatDisplayTime(remainingTime)}")
            .setSmallIcon(R.drawable.ic_timer_google_style)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH) // Try STOPWATCH category instead
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Add high priority
            .setOngoing(true) // Ensure it's marked as ongoing
        
        Log.d("TimerService", "Built notification with channel: $channelId, ongoing: true")
        return builder
    }

    private fun saveTimerState() {
        with(sharedPreferences.edit()) {
            when (val state = timerState.value) {
                is TimerState.Idle -> {
                    putBoolean(KEY_TIMER_RUNNING, false)
                    putBoolean(KEY_TIMER_FINISHED, false)
                }
                is TimerState.Running -> {
                    putBoolean(KEY_TIMER_RUNNING, true)
                    putBoolean(KEY_TIMER_FINISHED, false)
                    putLong(KEY_TIMER_END_TIME, System.currentTimeMillis() + state.remainingTime)
                    putInt(KEY_TIMER_DURATION, timerInitialValue.value.toInt())
                }
                is TimerState.Finished -> {
                    putBoolean(KEY_TIMER_RUNNING, false)
                    putBoolean(KEY_TIMER_FINISHED, true)
                }
            }
            commit()
        }
    }

    private fun restoreTimerState() {
        val isRunning = sharedPreferences.getBoolean(KEY_TIMER_RUNNING, false)
        val isFinished = sharedPreferences.getBoolean(KEY_TIMER_FINISHED, false)

        if (isRunning) {
            val endTime = sharedPreferences.getLong(KEY_TIMER_END_TIME, 0)
            val remainingTime = endTime - System.currentTimeMillis()
            if (remainingTime > 0) {
                val initialTime = sharedPreferences.getInt(KEY_TIMER_DURATION, 0).toLong()
                _timerInitialValue.value = initialTime
                startTimer(remainingTime)
            } else {
                _timerState.value =
                    TimerState.Finished(sharedPreferences.getInt(KEY_TIMER_DURATION, 0).toLong())
                saveTimerState()
            }
        } else if (isFinished) {
            _timerState.value =
                TimerState.Finished(sharedPreferences.getInt(KEY_TIMER_DURATION, 0).toLong())
        } else {
            _timerState.value = TimerState.Idle
        }
    }

    private fun requestTileUpdate() {
        lifecycleScope.launch {
            TileService.getUpdater(applicationContext)
                .requestUpdate(TimerTileService::class.java)
        }
    }

    inner class TimerBinder : Binder() {
        fun startTimer(duration: Long) = this@TimerService.startTimer(duration)
        fun stopTimer() = this@TimerService.stopTimer()
    }

    companion object {
        const val EXTRA_TIMER_DURATION = "com.frank.wearcompass.EXTRA_TIMER_DURATION"
        const val ACTION_STOP_TIMER = "com.frank.wearcompass.ACTION_STOP_TIMER"
        private const val PREFS_NAME = "timer_state"
        private const val KEY_TIMER_RUNNING = "timer_running"
        private const val KEY_TIMER_FINISHED = "timer_finished"
        private const val KEY_TIMER_DURATION = "timer_duration"
        private const val KEY_TIMER_END_TIME = "timer_end_time"

        private const val NOTIFICATION_ID = 123
    }
}