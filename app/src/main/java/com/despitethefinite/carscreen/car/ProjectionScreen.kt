package com.despitethefinite.carscreen.car

import android.content.pm.PackageManager
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.info.Speed
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.despitethefinite.carscreen.R
import com.despitethefinite.carscreen.WebConfig

/**
 * The single car screen. NavigationTemplate is used only because it is the one template
 * on Android Auto that hands the app a Surface; everything visible is drawn by [CarDisplay].
 */
class ProjectionScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val display = CarDisplay(carContext)

    init {
        lifecycle.addObserver(this)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(display)
    }

    override fun onCreate(owner: LifecycleOwner) {
        setUpSpeedMonitor()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        display.release()
    }

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(action(R.drawable.ic_home) { display.load(WebConfig.HOME_URL) })
            .addAction(action(R.drawable.ic_back) { display.back() })
            .addAction(action(R.drawable.ic_refresh) { display.reload() })
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .setBackgroundColor(CarColor.SECONDARY)
            .build()
    }

    private fun action(iconRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build())
            .setOnClickListener(onClick)
            .build()

    // ---- Safety: pause playback when the car starts moving (best effort, host permitting) ----

    private fun setUpSpeedMonitor() {
        if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_3) return
        if (ContextCompat.checkSelfPermission(carContext, CAR_SPEED) == PackageManager.PERMISSION_GRANTED) {
            startSpeedMonitor()
            return
        }
        val prefs = carContext.getSharedPreferences("carscreen", 0)
        if (prefs.getBoolean(PREF_SPEED_ASKED, false)) return
        prefs.edit().putBoolean(PREF_SPEED_ASKED, true).apply()
        try {
            carContext.requestPermissions(listOf(CAR_SPEED)) { granted, _ ->
                if (granted.contains(CAR_SPEED)) startSpeedMonitor()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Speed permission request failed", t)
        }
    }

    private fun startSpeedMonitor() {
        try {
            val hardware = carContext.getCarService(CarContext.HARDWARE_SERVICE) as CarHardwareManager
            hardware.carInfo.addSpeedListener(ContextCompat.getMainExecutor(carContext)) { speed: Speed ->
                val metersPerSecond = speed.rawSpeedMetersPerSecond.value
                    ?: speed.displaySpeedMetersPerSecond.value
                display.setMoving(metersPerSecond != null && metersPerSecond > MOVING_THRESHOLD_MPS)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Car speed not available on this host", t)
        }
    }

    private companion object {
        const val TAG = "ProjectionScreen"
        const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
        const val PREF_SPEED_ASKED = "speed_permission_asked"
        const val MOVING_THRESHOLD_MPS = 1.5f
    }
}
