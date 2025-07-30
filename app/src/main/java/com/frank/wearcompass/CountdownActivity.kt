package com.frank.wearcompass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

class CountdownActivity : ComponentActivity() {
    private val CHANNEL_ID = "timer_channel"

    companion object {
        private const val PREFS_NAME = "timer_state"
        private const val KEY_TIMER_RUNNING = "timer_running"
        private const val KEY_TIMER_FINISHED = "timer_finished"
        private const val KEY_TIMER_DURATION = "timer_duration"
        private const val KEY_TIMER_END_TIME = "timer_end_time"

        const val EXTRA_TIMER_STATE = "timer_state"
        const val STATE_RUNNING = 0
        const val STATE_FINISHED = 1

        fun isTimerRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isTimerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false)
            val isTimerFinished = prefs.getBoolean(KEY_TIMER_FINISHED, false)
            return isTimerRunning || isTimerFinished
        }

        fun getRemainingTime(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isTimerFinished = prefs.getBoolean(KEY_TIMER_FINISHED, false)
            if (isTimerFinished) {
                return 0
            }
            val endTime = prefs.getLong(KEY_TIMER_END_TIME, 0)
            val currentTime = System.currentTimeMillis()
            val remainingMillis = endTime - currentTime
            return if (remainingMillis > 0) (remainingMillis / 1000).toInt() else 0
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for timer status"
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getOriginalTimerDuration(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TIMER_DURATION, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTimerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false)
        val isTimerFinished = prefs.getBoolean(KEY_TIMER_FINISHED, false)

        var timerLength: Int
        var timerState: Int
        var originalDuration: Int

        val requestedTimerLength = intent.getIntExtra("timer_length", 60)

        if (requestedTimerLength == -1) {
            if (isTimerRunning && !isTimerFinished) {
                timerLength = getRemainingTime(this)
                timerState = STATE_RUNNING
                originalDuration = getOriginalTimerDuration()
            } else if (isTimerFinished) {
                timerLength = 0
                timerState = STATE_FINISHED
                originalDuration = getOriginalTimerDuration()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
                return
            }
        } else if (isTimerRunning && !isTimerFinished) {
            timerLength = getRemainingTime(this)
            timerState = STATE_RUNNING
            originalDuration = getOriginalTimerDuration()
        } else if (isTimerFinished) {
            timerLength = 0
            timerState = STATE_FINISHED
            originalDuration = getOriginalTimerDuration()
        } else {
            timerLength = requestedTimerLength
            timerState = intent.getIntExtra(EXTRA_TIMER_STATE, STATE_RUNNING)
            originalDuration = timerLength

            if (timerState == STATE_RUNNING) {
                val serviceIntent = Intent(this, TimerService::class.java)
                serviceIntent.putExtra(TimerService.EXTRA_TIMER_DURATION, timerLength)
                startService(serviceIntent)
            }
        }

        createNotificationChannel()

        setContent {
            CountdownScreen(
                initialTime = timerLength,
                originalDuration = originalDuration,
                initialStateIsFinished = timerState == STATE_FINISHED,
                onTimerStarted = { /* Service handles this */ },
                onTimerFinished = { /* Service handles this */ },
                onStopPressed = {
                    if (timerState == STATE_FINISHED) {
                        stopTimer()
                    } else {
                        stopTimerAndGoToPicker()
                    }
                },
                onRestartPressed = { restartTimer(originalDuration) }
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTimerFinished = prefs.getBoolean(KEY_TIMER_FINISHED, false)

        if (isTimerFinished) {
            finishAffinity()
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            finish()
        }
    }

    private fun stopTimer() {
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_STOP_TIMER
        startService(serviceIntent)
        finishAffinity()
    }

    private fun stopTimerAndGoToPicker() {
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_STOP_TIMER
        startService(serviceIntent)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun restartTimer(duration: Int) {
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.action = TimerService.ACTION_STOP_TIMER
        startService(serviceIntent)

        val newIntent = Intent(this, CountdownActivity::class.java)
        newIntent.putExtra("timer_length", duration)
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(newIntent)
        finish()
    }
}

@Composable
fun CountdownScreen(
    initialTime: Int,
    originalDuration: Int,
    initialStateIsFinished: Boolean,
    onTimerStarted: () -> Unit,
    onTimerFinished: () -> Unit,
    onStopPressed: () -> Unit,
    onRestartPressed: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(initialTime) }
    var preciseTimeLeft by remember { mutableStateOf(initialTime.toFloat()) }
    var isRunning by remember { mutableStateOf(!initialStateIsFinished) }
    var isFinished by remember { mutableStateOf(initialStateIsFinished) }
    var timer: CountDownTimer? by remember { mutableStateOf(null) }

    LaunchedEffect(initialStateIsFinished) {
        if (initialStateIsFinished) {
            onTimerFinished()
        }
    }

    val effectiveInitialTime = originalDuration.toFloat()
    val targetProgress by remember {
        derivedStateOf {
            if (isFinished) 0f else if (preciseTimeLeft > 0f) {
                val progress = preciseTimeLeft / effectiveInitialTime
                if (progress > 1f) 1f else progress
            } else 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "flashTransition")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "flashAlpha"
    )

    DisposableEffect(isRunning) {
        if (isRunning) {
            if (!initialStateIsFinished) {
                onTimerStarted()
            }

            timer = object : CountDownTimer((timeLeft * 1000).toLong(), 100) {
                override fun onTick(millisUntilFinished: Long) {
                    preciseTimeLeft = millisUntilFinished / 1000f
                    timeLeft = (millisUntilFinished / 1000).toInt()
                }
                override fun onFinish() {
                    timeLeft = 0
                    preciseTimeLeft = 0f
                    isRunning = false
                    isFinished = true
                    onTimerFinished()
                }
            }.start()
        } else {
            timer?.cancel()
        }
        onDispose {
            timer?.cancel()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = flashAlpha))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "TIME'S UP!",
                        fontSize = 28.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )

                    Button(
                        onClick = onStopPressed,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .fillMaxWidth(0.7f)
                            .height(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension / 2) - strokeWidth
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                    if (animatedProgress > 0f) {
                        val sweepAngle = animatedProgress * 360f
                        drawArc(
                            color = Color.Blue,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                        fontSize = 36.sp,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = onStopPressed) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Black)
                            )
                        }
                        Button(onClick = onRestartPressed) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Restart",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:wearos_large_round")
@Composable
fun CountdownLargeRoundPreview() {
    CountdownScreen(
        initialTime = 120,
        originalDuration = 120,
        initialStateIsFinished = false,
        onTimerStarted = {},
        onTimerFinished = {},
        onStopPressed = {},
        onRestartPressed = {}
    )
}
