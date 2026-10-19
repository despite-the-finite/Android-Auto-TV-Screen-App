package com.despitethefinite.carscreen.car

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.despitethefinite.carscreen.ProjectionController

/**
 * Owns the car-side rendering: a private VirtualDisplay backed by the Surface Android Auto
 * gives us, and a [WebPresentation] shown on that display. Touch callbacks from the host are
 * translated into events on the presentation's view hierarchy.
 */
class CarDisplay(private val carContext: CarContext) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: WebPresentation? = null
    private var moving = false

    override fun onSurfaceAvailable(container: SurfaceContainer) {
        val surface = container.surface ?: return
        tearDown()

        val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
        val display = try {
            displayManager.createVirtualDisplay(
                "CarScreen", container.width, container.height, container.dpi, surface, flags,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Could not create virtual display", t)
            return
        }
        virtualDisplay = display

        presentation = WebPresentation(carContext, display.display).also {
            it.show()
            it.setMovingOverlay(moving)
            it.load(ProjectionController.lastUrl)
        }
        ProjectionController.attach(this, container.width, container.height)
        Log.i(TAG, "Car surface ${container.width}x${container.height} @ ${container.dpi} dpi")
    }

    override fun onSurfaceDestroyed(container: SurfaceContainer) {
        ProjectionController.detach(this)
        tearDown()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) = Unit
    override fun onStableAreaChanged(stableArea: Rect) = Unit

    override fun onClick(x: Float, y: Float) {
        val view = presentation?.window?.decorView ?: return
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        presentation?.webView?.scrollBy(distanceX.toInt(), distanceY.toInt())
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        presentation?.webView?.flingScroll(-velocityX.toInt(), -velocityY.toInt())
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) = Unit

    // ---- Commands from the phone UI and the car action strip ----

    fun load(url: String) {
        presentation?.load(url)
    }

    fun back() {
        presentation?.webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun reload() {
        presentation?.webView?.reload()
    }

    fun evaluateJs(script: String) {
        presentation?.webView?.evaluateJavascript(script, null)
    }

    fun setMoving(isMoving: Boolean) {
        if (moving == isMoving) return
        moving = isMoving
        presentation?.setMovingOverlay(isMoving)
        if (isMoving) evaluateJs(com.despitethefinite.carscreen.WebConfig.JS_PAUSE)
    }

    fun release() {
        ProjectionController.detach(this)
        tearDown()
    }

    private fun tearDown() {
        presentation?.let {
            runCatching { it.dismiss() }
            it.destroyWebView()
        }
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private companion object {
        const val TAG = "CarDisplay"
    }
}
