package com.frank.wearcompass

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class BasicTimerComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortTextComplication()
            ComplicationType.MONOCHROMATIC_IMAGE -> createMonochromaticImageComplication()
            else -> null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationDataSourceService.ComplicationRequestListener
    ) {
        val complicationData = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextComplication()
            ComplicationType.MONOCHROMATIC_IMAGE -> createMonochromaticImageComplication()
            else -> null
        }
        
        listener.onComplicationData(complicationData)
    }

    private fun createShortTextComplication(): ComplicationData {
        // Check if timer is already running
        val isTimerRunning = CountdownActivity.isTimerRunning(this)
        
        val pendingIntent = if (isTimerRunning) {
            // If timer is running, just launch the app to show current timer
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, CountdownActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // If no timer is running, start a 1-minute timer
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, CountdownActivity::class.java).apply {
                    putExtra("timer_length", 60) // 1 minute
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("1m").build(),
            contentDescription = PlainComplicationText.Builder("1 Minute Timer").build()
        )
            .setTapAction(pendingIntent)
            .build()
    }

    private fun createMonochromaticImageComplication(): ComplicationData {
        // Check if timer is already running
        val isTimerRunning = CountdownActivity.isTimerRunning(this)
        
        val pendingIntent = if (isTimerRunning) {
            // If timer is running, just launch the app to show current timer
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, CountdownActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // If no timer is running, start a 1-minute timer
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, CountdownActivity::class.java).apply {
                    putExtra("timer_length", 60) // 1 minute
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return try {
            MonochromaticImageComplicationData.Builder(
                monochromaticImage = MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_timer_google_style)
                ).build(),
                contentDescription = PlainComplicationText.Builder("1 Minute Timer").build()
            )
                .setTapAction(pendingIntent)
                .build()
        } catch (e: Exception) {
            // Fallback to text if icon fails
            createShortTextComplication()
        }
    }
} 