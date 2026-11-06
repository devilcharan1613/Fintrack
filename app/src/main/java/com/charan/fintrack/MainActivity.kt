package com.charan.fintrack

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.setSupportZoom(false)
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
                )
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                evaluateJavascript(
                    "window.finTrackApplyInsets(${safe.left},${safe.top},${safe.right},${safe.bottom},${ime.bottom});",
                    null
                )
                // Insets are consumed by CSS so the WebView and CSS do not both add them.
                view.setPadding(0, 0, 0, 0)
                insets
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else (this@MainActivity as Activity).finish()
            }
        })
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
