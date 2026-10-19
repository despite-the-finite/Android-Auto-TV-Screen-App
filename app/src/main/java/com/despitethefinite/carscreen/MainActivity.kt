package com.despitethefinite.carscreen

import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Phone-side companion. Hosts its own WebView so the user can log in to streaming sites with
 * the phone keyboard; the car WebView shares the same cookie jar. Also exposes remote-control
 * buttons for the car screen, which has no keyboard of its own.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var phoneWeb: WebView
    private lateinit var urlField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val presets = findViewById<LinearLayout>(R.id.presets)
        val typeField = findViewById<EditText>(R.id.typeText)
        urlField = findViewById(R.id.url)
        phoneWeb = findViewById(R.id.phoneWeb)

        val contentRoot = findViewById<ViewGroup>(android.R.id.content)
        WebConfig.apply(phoneWeb, StreamingChromeClient(contentRoot, phoneWeb)) { url -> urlField.setText(url) }

        WebConfig.presets.forEach { (name, url) ->
            presets.addView(Button(this).apply {
                text = name
                setOnClickListener { phoneWeb.loadUrl(url) }
            })
        }

        val go = { phoneWeb.loadUrl(WebConfig.normalizeUrl(urlField.text.toString())) }
        findViewById<Button>(R.id.go).setOnClickListener { go() }
        urlField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { go(); true } else false
        }

        findViewById<Button>(R.id.sendToCar).setOnClickListener {
            val url = phoneWeb.url ?: WebConfig.normalizeUrl(urlField.text.toString())
            ProjectionController.load(url)
        }
        findViewById<Button>(R.id.type).setOnClickListener {
            ProjectionController.typeText(typeField.text.toString())
            typeField.text.clear()
        }
        findViewById<Button>(R.id.enter).setOnClickListener { ProjectionController.pressEnter() }
        findViewById<Button>(R.id.carBack).setOnClickListener { ProjectionController.back() }
        findViewById<Button>(R.id.carReload).setOnClickListener { ProjectionController.reload() }
        findViewById<Button>(R.id.carPlay).setOnClickListener { ProjectionController.play() }
        findViewById<Button>(R.id.carPause).setOnClickListener { ProjectionController.pause() }

        ProjectionController.status.observe(this) { s ->
            status.text = if (s.active) getString(R.string.status_active, s.width, s.height)
            else getString(R.string.status_idle)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (phoneWeb.canGoBack()) phoneWeb.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        if (savedInstanceState == null) phoneWeb.loadUrl(WebConfig.HOME_URL)
    }

    override fun onDestroy() {
        phoneWeb.destroy()
        super.onDestroy()
    }
}
