package com.frank.wearcompass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity(), SensorEventListener {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val ALPHA = 0.25f // Low-pass filter constant
    private val TAG = "MainActivity"

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var geomagneticField: GeomagneticField? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private var bearing by mutableStateOf(0f)
    private var sensorsRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissions()

        setContent {
            MaterialTheme {
                Compass(bearing = bearing)
            }
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            setupSensors()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setupSensors()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!sensorsRegistered) {
            setupSensors()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterSensors()
    }

    private fun setupSensors() {
        unregisterSensors() // Ensure clean registration
        
        accelerometer?.also { sensor ->
            val success = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            if (!success) {
                Log.e(TAG, "Failed to register accelerometer")
            }
        }
        magnetometer?.also { sensor ->
            val success = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            if (!success) {
                Log.e(TAG, "Failed to register magnetometer")
            }
        }
        sensorsRegistered = true
        
        // Get location for magnetic declination
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    geomagneticField = GeomagneticField(
                        location.latitude.toFloat(),
                        location.longitude.toFloat(),
                        location.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    Log.d(TAG, "Magnetic declination: ${geomagneticField?.declination}°")
                }
            }
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
        sensorsRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                gravity = lowPass(event.values.clone(), gravity)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagnetic = lowPass(event.values.clone(), geomagnetic)
            }
        }

        if (gravity != null && geomagnetic != null) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthInRadians = orientationAngles[0]
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                
                // Convert to 0-360 range
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                
                // Apply magnetic declination correction to get true north
                geomagneticField?.let { field ->
                    val declination = field.declination
                    azimuthInDegrees -= declination
                    
                    // Keep in 0-360 range
                    if (azimuthInDegrees < 0) {
                        azimuthInDegrees += 360f
                    } else if (azimuthInDegrees >= 360f) {
                        azimuthInDegrees -= 360f
                    }
                }
                
                bearing = azimuthInDegrees
                Log.d(TAG, "Bearing: $bearing°")
            } else {
                Log.w(TAG, "Failed to get rotation matrix")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Log.d(TAG, "Sensor accuracy: HIGH")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Log.d(TAG, "Sensor accuracy: MEDIUM")
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Log.d(TAG, "Sensor accuracy: LOW")
            SensorManager.SENSOR_STATUS_UNRELIABLE -> Log.w(TAG, "Sensor accuracy: UNRELIABLE")
        }
    }

    private fun lowPass(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        for (i in input.indices) {
            output[i] = output[i] + ALPHA * (input[i] - output[i])
        }
        return output
    }
}

@Composable
fun Compass(bearing: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_arrow),
            contentDescription = "Compass Needle",
            modifier = Modifier
                .fillMaxSize(0.5f)
                .rotate(degrees = -bearing),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
        )
    }
}

@Preview(
    device = Devices.WEAR_OS_LARGE_ROUND,
    showSystemUi = true
)
@Composable
fun CompassPreview() {
    MaterialTheme {
        Compass(bearing = 45f)
    }
}
