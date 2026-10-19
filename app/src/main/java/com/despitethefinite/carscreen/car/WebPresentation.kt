package com.despitethefinite.carscreen.car

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import com.despitethefinite.carscreen.R
import com.despitethefinite.carscreen.StreamingChromeClient
import com.despitethefinite.carscreen.WebConfig

/** Full-screen WebView rendered onto the car's virtual display. */
class WebPresentation(outerContext: Context, display: Display) :
    Presentation(outerContext, display, android.R.style.Theme_Material_NoActionBar_Fullscreen) {

    lateinit var webView: WebView
        private set
    private lateinit var root: FrameLayout
    private lateinit var movingOverlay: TextView
    private var pendingUrl: String? = null
    private var showMoving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setBackgroundDrawableResource(android.R.color.black)
        }

        root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(context)
        root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        movingOverlay = TextView(context).apply {
            text = context.getString(R.string.moving_warning)
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(230, 0, 0, 0))
            visibility = if (showMoving) View.VISIBLE else View.GONE
        }
        root.addView(movingOverlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        WebConfig.apply(webView, StreamingChromeClient(root, webView))
        setContentView(root)

        pendingUrl?.let { webView.loadUrl(it) }
        pendingUrl = null
    }

    fun load(url: String) {
        if (::webView.isInitialized) webView.loadUrl(url) else pendingUrl = url
    }

    fun setMovingOverlay(visible: Boolean) {
        showMoving = visible
        if (::movingOverlay.isInitialized) movingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun destroyWebView() {
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }
}
