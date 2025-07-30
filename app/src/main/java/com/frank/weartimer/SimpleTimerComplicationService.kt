package com.frank.wearcompass

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class SimpleTimerComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type)
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationDataSourceService.ComplicationRequestListener
    ) {
        listener.onComplicationData(createComplicationData(request.complicationType))
    }

    private fun createComplicationData(type: ComplicationType): ComplicationData? {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, CountdownActivity::class.java).apply {
                putExtra("timer_length", 60) // 1 minute
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "1m"
        val contentDescription = "1 Minute Timer"

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("1 Minute Timer").build(),
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 1f,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
                )
                    .setText(PlainComplicationText.Builder(text).build())
                    .setTapAction(pendingIntent)
                    .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                try {
                    MonochromaticImageComplicationData.Builder(
                        monochromaticImage = MonochromaticImage.Builder(
                            Icon.createWithResource(this, R.drawable.ic_timer_simple)
                        ).build(),
                        contentDescription = PlainComplicationText.Builder(contentDescription).build()
                    )
                        .setTapAction(pendingIntent)
                        .build()
                } catch (e: Exception) {
                    // Fallback to text if icon fails
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(text).build(),
                        contentDescription = PlainComplicationText.Builder(contentDescription).build()
                    )
                        .setTapAction(pendingIntent)
                        .build()
                }
            }
            else -> null
        }
    }
} 