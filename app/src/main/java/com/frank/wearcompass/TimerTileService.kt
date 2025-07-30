package com.frank.wearcompass

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val RESOURCES_VERSION = "1"
private const val COUNTDOWN_ACTIVITY_CLASS_NAME = "com.frank.wearcompass.CountdownActivity"
private const val MAIN_ACTIVITY_CLASS_NAME = "com.frank.wearcompass.MainActivity"

class TimerTileService : TileService() {
    companion object {
        fun requestTileUpdate(context: Context) {
            getUpdater(context).requestUpdate(TimerTileService::class.java)
        }
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val isTimerRunning = CountdownActivity.isTimerRunning(applicationContext)
        val remainingTime = CountdownActivity.getRemainingTime(applicationContext)

        val layout = if (isTimerRunning) {
            val minutes = remainingTime / 60
            val seconds = remainingTime % 60
            val timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

            val openCountdownActivity = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(COUNTDOWN_ACTIVITY_CLASS_NAME)
                    .addKeyToExtraMapping("timer_length", ActionBuilders.intExtra(-1))
                    .build()
            ).build()

            LayoutElementBuilders.Layout.Builder()
                .setRoot(
                    LayoutElementBuilders.Box.Builder()
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setOnClick(openCountdownActivity)
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(timeText)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(DimensionBuilders.sp(48f))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        } else {
            LayoutElementBuilders.Layout.Builder()
                .setRoot(
                    LayoutElementBuilders.Column.Builder()
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText("Quick Timer")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(DimensionBuilders.sp(18f))
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(DimensionBuilders.dp(8f))
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Row.Builder()
                                .addContent(createTimerButton("1m", 60))
                                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
                                .addContent(createTimerButton("2m", 120))
                                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
                                .addContent(createTimerButton("5m", 300))
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(DimensionBuilders.dp(8f))
                                .build()
                        )
                        .addContent(
                            createTimerButton("Custom", null)
                        )
                        .build()
                )
                .build()
        }

        val tileBuilder = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder().addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(layout)
                        .build()
                ).build()
            )

        if (isTimerRunning) {
            // Set a very short freshness interval for real-time updates
            tileBuilder.setFreshnessIntervalMillis(500) // Update every 500ms
        }

        return Futures.immediateFuture(tileBuilder.build())
    }

    private fun createTimerButton(text: String, durationSeconds: Int?): LayoutElementBuilders.LayoutElement {
        val activity = if (durationSeconds != null) {
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(COUNTDOWN_ACTIVITY_CLASS_NAME)
                .addKeyToExtraMapping(
                    "timer_length",
                    ActionBuilders.intExtra(durationSeconds)
                )
                .build()
        } else {
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(MAIN_ACTIVITY_CLASS_NAME)
                .build()
        }

        val action = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(activity)
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setOnClick(action)
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setAll(DimensionBuilders.dp(15f))
                            .build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF749EFA.toInt()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(DimensionBuilders.dp(24f))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(text)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.sp(18f))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources> {
        val resources = androidx.wear.tiles.ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }
}