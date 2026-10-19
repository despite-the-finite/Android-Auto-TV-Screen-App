package com.despitethefinite.carscreen

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * Shared WebView setup used by both the phone activity and the car presentation.
 * Both WebViews live in the same process, so cookies (and therefore logins) are shared.
 */
object WebConfig {

    /** Streaming sites serve their full web player to desktop browsers, not to mobile ones. */
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    const val HOME_URL = "https://www.netflix.com/browse"

    val presets: List<Pair<String, String>> = listOf(
        "Netflix" to "https://www.netflix.com/browse",
        "YouTube" to "https://www.youtube.com",
        "Prime Video" to "https://www.primevideo.com",
        "Disney+" to "https://www.disneyplus.com",
        "Max" to "https://play.max.com",
        "Plex" to "https://app.plex.tv",
        "Tubi" to "https://tubitv.com",
    )

    const val JS_PLAY = "document.querySelectorAll('video').forEach(function(v){v.play();});"
    const val JS_PAUSE = "document.querySelectorAll('video').forEach(function(v){v.pause();});"

    /** Turns free text into a URL: bare words become a search, bare hosts get https://. */
    fun normalizeUrl(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return HOME_URL
        if (text.startsWith("http://") || text.startsWith("https://")) return text
        val looksLikeHost = !text.contains(' ') && text.contains('.')
        return if (looksLikeHost) "https://$text"
        else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(text, "UTF-8")
    }

    /** Appends [text] to whatever page element currently has focus, in a way React-style inputs notice. */
    fun jsType(text: String): String {
        val quoted = JSONObject.quote(text)
        return """
            (function(){
              var e = document.activeElement;
              if (!e) return;
              if (e.isContentEditable) { document.execCommand('insertText', false, $quoted); return; }
              var proto = Object.getPrototypeOf(e);
              var desc = Object.getOwnPropertyDescriptor(proto, 'value');
              var next = (e.value || '') + $quoted;
              if (desc && desc.set) { desc.set.call(e, next); } else { e.value = next; }
              e.dispatchEvent(new Event('input', { bubbles: true }));
              e.dispatchEvent(new Event('change', { bubbles: true }));
            })();
        """.trimIndent()
    }

    const val JS_ENTER = """
        (function(){
          var e = document.activeElement;
          if (!e) return;
          ['keydown','keypress','keyup'].forEach(function(t){
            e.dispatchEvent(new KeyboardEvent(t, { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
          });
          if (e.form) { if (e.form.requestSubmit) e.form.requestSubmit(); else e.form.submit(); }
        })();
    """

    @SuppressLint("SetJavaScriptEnabled")
    fun apply(webView: WebView, chromeClient: WebChromeClient, onUrlChanged: ((String) -> Unit)? = null) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = DESKTOP_UA
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.webChromeClient = chromeClient
        webView.webViewClient = object : WebViewClient() {
            // Keep every navigation inside this WebView instead of bouncing to Chrome.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

            override fun onPageFinished(view: WebView, url: String) {
                onUrlChanged?.invoke(url)
            }
        }
    }
}

/**
 * WebChromeClient that (1) grants the DRM permission streaming players need for Widevine
 * and (2) hosts HTML5 fullscreen video inside [container].
 */
class StreamingChromeClient(
    private val container: ViewGroup,
    private val webView: WebView,
) : WebChromeClient() {

    private var customView: View? = null
    private var customCallback: CustomViewCallback? = null

    override fun onPermissionRequest(request: PermissionRequest) {
        val wanted = request.resources.filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
        if (wanted.isEmpty()) request.deny() else request.grant(wanted.toTypedArray())
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customCallback = callback
        webView.visibility = View.GONE
        container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun onHideCustomView() {
        customView?.let { container.removeView(it) }
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
        webView.visibility = View.VISIBLE
    }
}
