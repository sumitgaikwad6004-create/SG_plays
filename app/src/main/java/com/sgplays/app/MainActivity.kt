package com.sgplays.app

import android.app.Activity
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var toolbar: LinearLayout
    private lateinit var root: FrameLayout
    private lateinit var progress: ProgressBar

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var oldOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private val purple = Color.rgb(124, 92, 252)
    private val dark = Color.rgb(16, 17, 22)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        buildInterface()
        setupBrowser()

        val saved = getPreferences(MODE_PRIVATE)
            .getString("last_url", null)

        web.loadUrl(saved ?: "https://studyrays.com")
    }

    private fun buildInterface() {
        root = FrameLayout(this).apply {
            setBackgroundColor(dark)
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(5, 8, 5, 8)
        }

        fun button(label: String, action: () -> Unit): TextView {
            return TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(8, 12, 8, 12)
                setOnClickListener { action() }
            }
        }

        toolbar.addView(button("‹") {
            if (web.canGoBack()) web.goBack()
        })

        toolbar.addView(button("›") {
            if (web.canGoForward()) web.goForward()
        })

        toolbar.addView(button("↻") {
            web.reload()
        })

        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            hint = "Enter lecture URL"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_URI
            setPadding(10, 4, 10, 4)
            setBackgroundColor(Color.rgb(38, 39, 48))
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO

            setOnEditorActionListener { _, _, _ ->
                loadAddress()
                true
            }
        }

        toolbar.addView(address, LinearLayout.LayoutParams(
            0, 48, 1f
        ))

        toolbar.addView(button("COPY") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE)
                as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText("Lecture URL", web.url ?: "")
            )

            Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show()
        })

        toolbar.addView(button("PASTE") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE)
                as ClipboardManager

            val clip = clipboard.primaryClip

            if (clip != null && clip.itemCount > 0) {
                address.setText(clip.getItemAt(0).coerceToText(this))
                loadAddress()
            }
        })

        toolbar.addView(button("HOME") {
            web.loadUrl("https://studyrays.com")
        })

        progress = ProgressBar(
            this, null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progressTintList =
                android.content.res.ColorStateList.valueOf(purple)
            visibility = View.GONE
        }

        web = WebView(this)

        main.addView(toolbar)

        main.addView(progress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 3
        ))

        main.addView(web, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(main)
        setContentView(root)
    }

    private fun setupBrowser() {
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        web.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val scheme = request.url.scheme
                return scheme != "https" && scheme != "http"
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {
                address.setText(url)

                getPreferences(MODE_PRIVATE).edit()
                    .putString("last_url", url)
                    .apply()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@MainActivity,
                        "Page loading failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {
                progress.progress = newProgress

                progress.visibility =
                    if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback
            ) {
                if (fullscreenView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                fullscreenView = view
                fullscreenCallback = callback
                oldOrientation = requestedOrientation

                toolbar.visibility = View.GONE
                progress.visibility = View.GONE

                root.addView(view, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))

                requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )

                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }
    }

    private fun loadAddress() {
        var url = address.text.toString().trim()

        if (url.isEmpty()) return

        if (!url.startsWith("https://") &&
            !url.startsWith("http://")) {
            url = "https://$url"
        }

        try {
            val uri = android.net.Uri.parse(url)

            if (uri.scheme != "https" && uri.scheme != "http") {
                return
            }

            web.loadUrl(url)
            address.clearFocus()

            (getSystemService(INPUT_METHOD_SERVICE)
                as InputMethodManager)
                .hideSoftInputFromWindow(address.windowToken, 0)

        } catch (e: Exception) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exitFullscreen() {
        val view = fullscreenView ?: return

        root.removeView(view)
        fullscreenView = null

        toolbar.visibility = View.VISIBLE

        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        requestedOrientation = oldOrientation

        val callback = fullscreenCallback
        fullscreenCallback = null
        callback?.onCustomViewHidden()
    }

    @Deprecated("Legacy Android back handling")
    override fun onBackPressed() {
        if (fullscreenView != null) {
            exitFullscreen()
        } else if (web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        web.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        if (fullscreenView != null) exitFullscreen()
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}
