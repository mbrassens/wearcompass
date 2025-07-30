package com.frank.wearcompass

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Scaffold
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.content.Context

class MainActivity : ComponentActivity() {
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if a timer is already running
        val isTimerRunning = isTimerRunning()
        
        if (isTimerRunning) {
            // Redirect to CountdownActivity if timer is running
            val intent = Intent(this, CountdownActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        } else {
            // Show timer picker if no timer is running
            setContent {
                StartTimerScreen { timerLength ->
                    val intent = Intent(this, CountdownActivity::class.java)
                    intent.putExtra("timer_length", timerLength)
                    startActivity(intent)
                    finish() // Close MainActivity when starting timer
                }
            }
        }
    }
    
    private fun isTimerRunning(): Boolean {
        // Check if CountdownActivity is in the foreground or if there's an ongoing timer
        val prefs = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
        val isTimerRunning = prefs.getBoolean("timer_running", false)
        val isTimerFinished = prefs.getBoolean("timer_finished", false)
        
        // Return true if timer is running OR finished (both should redirect to CountdownActivity)
        return isTimerRunning || isTimerFinished
    }
}

@Composable
fun StartTimerScreen(onStart: (Int) -> Unit) {
    val minutesOptions = (0..59).toList()
    val secondsOptions = (0..59).toList()

    val minutesPickerState = rememberPickerState(initialNumberOfOptions = minutesOptions.size, initiallySelectedOption = 1)
    val secondsPickerState = rememberPickerState(initialNumberOfOptions = secondsOptions.size, initiallySelectedOption = 0)

    val selectedMinutes by remember {
        derivedStateOf { minutesOptions[minutesPickerState.selectedOption] }
    }
    val selectedSeconds by remember {
        derivedStateOf { secondsOptions[secondsPickerState.selectedOption] }
    }

    val focusRequester = remember { FocusRequester() }

    Scaffold(
        timeText = { TimeText() },
        vignette = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy((-16).dp), // Negative spacing to bring pickers closer
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "mins",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Picker(
                        modifier = Modifier
                            .width(60.dp)
                            .height(80.dp)
                            .focusRequester(focusRequester),
                        state = minutesPickerState,
                        contentDescription = "minutes"
                    ) { index ->
                        Text(
                            text = minutesOptions[index].toString().padStart(2, '0'),
                            fontSize = 36.sp, // Increased font size from 26.sp to 30.sp
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Separator aligned with picker numbers
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ":",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "secs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Picker(
                        modifier = Modifier
                            .width(60.dp)
                            .height(80.dp),
                        state = secondsPickerState,
                        contentDescription = "seconds"
                    ) { index ->
                        Text(
                            text = secondsOptions[index].toString().padStart(2, '0'),
                            fontSize = 36.sp, // Increased font size from 26.sp to 30.sp
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val totalSeconds = selectedMinutes * 60 + selectedSeconds
                    if (totalSeconds > 0) {
                        onStart(totalSeconds)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
            ) {
                Text(
                    "Start Timer",
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
