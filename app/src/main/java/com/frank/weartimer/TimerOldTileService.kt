package com.frank.wearcompass

import android.content.Context
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val OLD_RESOURCES_VERSION = "1"
private const val OLD_COUNTDOWN_ACTIVITY_CLASS_NAME = "com.frank.wearcompass.CountdownActivity"
private const val OLD_MAIN_ACTIVITY_CLASS_NAME = "com.frank.wearcompass.MainActivity"

class TimerOldTileService : TileService() {

    companion object {
        fun requestTileUpdate(context: Context) {
            getUpdater(context).requestUpdate(TimerOldTileService::class.java)
        }
    }

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<TileBuilders.Tile> {
        val isTimerRunning = CountdownActivity.isTimerRunning(applicationContext)
        val deviceParams = requestParams.deviceParameters

        val layout = if (isTimerRunning) {
            val remainingTime = CountdownActivity.getRemainingTime(applicationContext)
            val minutes = remainingTime / 60
            val seconds = remainingTime % 60
            val timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

            val openCountdownActivity = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(OLD_COUNTDOWN_ACTIVITY_CLASS_NAME)
                    .addKeyToExtraMapping("timer_length", ActionBuilders.intExtra(-1))
                    .build()
            ).build()

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
                                .setSize(DimensionBuilders.SpProp.Builder().setValue(48f).build())
                                .build()
                        )
                        .build()
                )
                .build()
        } else {
            if (deviceParams == null) {
                // Fallback layout if device parameters are not available
                LayoutElementBuilders.Text.Builder().setText("Cannot load tile on this device.").build()
            } else {
                val oneMinAction = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(OLD_COUNTDOWN_ACTIVITY_CLASS_NAME)
                        .addKeyToExtraMapping("timer_length", ActionBuilders.intExtra(60))
                        .build()
                ).build()

                val twoMinAction = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(OLD_COUNTDOWN_ACTIVITY_CLASS_NAME)
                        .addKeyToExtraMapping("timer_length", ActionBuilders.intExtra(120))
                        .build()
                ).build()

                val fiveMinAction = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(OLD_COUNTDOWN_ACTIVITY_CLASS_NAME)
                        .addKeyToExtraMapping("timer_length", ActionBuilders.intExtra(300))
                        .build()
                ).build()

                val customAction = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(OLD_MAIN_ACTIVITY_CLASS_NAME)
                        .build()
                ).build()

                val oneMinButton = createCustomButton("1m", oneMinAction, 18f, 15f)
                val twoMinButton = createCustomButton("2m", twoMinAction, 18f, 15f)
                val fiveMinButton = createCustomButton("5m", fiveMinAction, 18f, 15f)
                val customButton = createCustomButton("Custom", customAction, 18f, 15f)


                val buttonLayout = LayoutElementBuilders.Column.Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.DpProp.Builder().setValue(8f).build()).build())
                    .addContent(
                        LayoutElementBuilders.Row.Builder()
                            .addContent(oneMinButton)
                            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.DpProp.Builder().setValue(8f).build()).build())
                            .addContent(twoMinButton)
                            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.DpProp.Builder().setValue(8f).build()).build())
                            .addContent(fiveMinButton)
                            .build()
                    )
                    .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.DpProp.Builder().setValue(8f).build()).build())
                    .addContent(customButton)
                    .build()


                PrimaryLayout.Builder(deviceParams)
                    .setPrimaryLabelTextContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("Quick Timer")
                            .build()
                    )
                    .setContent(buttonLayout)
                    .build()
            }
        }

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(OLD_RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder().addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
                        .build()
                ).build()
            )

        if (isTimerRunning) {
            tile.setFreshnessIntervalMillis(TimeUnit.SECONDS.toMillis(1))
        }

        return Futures.immediateFuture(tile.build())
    }

    private fun createCustomButton(text: String, action: ActionBuilders.LaunchAction, fontSize: Float, padding: Float): LayoutElementBuilders.Box {
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
                            .setAll(DimensionBuilders.DpProp.Builder().setValue(padding).build())
                            .build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF749EFA.toInt()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(DimensionBuilders.DpProp.Builder().setValue(24f).build())
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
                            .setSize(DimensionBuilders.SpProp.Builder().setValue(fontSize).build())
                            .build()
                    )
                    .build()
            )
            .build()
    }


    @Deprecated("onResourcesRequest is deprecated for this tile")
    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(OLD_RESOURCES_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }
}