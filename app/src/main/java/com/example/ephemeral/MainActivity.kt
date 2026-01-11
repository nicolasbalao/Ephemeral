package com.example.ephemeral

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var tabManager: TabManager
    private lateinit var urlBar: EditText

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val container = findViewById<FrameLayout>(R.id.webview_container)

        urlBar = findViewById<EditText>(R.id.urlBar)


        val webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                urlBar.setText(url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }
        val webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            private var previousWebView: WebView? = null

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                val currentWebView = tabManager.getCurrentTab()?.webView ?: return
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                previousWebView = currentWebView
                customView = view
                customViewCallback = callback

                // Cache la WebView normale
                currentWebView.visibility = View.GONE

                // Ajoute la vue fullscreen dans le decor view
                val decor = window.decorView as FrameLayout
                decor.addView(
                    view, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // Fullscreen immersive
                window.insetsController?.hide(WindowInsets.Type.systemBars())
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onHideCustomView() {
                customView?.let {
                    (window.decorView as FrameLayout).removeView(it)
                    customView = null
                }

                window.insetsController?.show(WindowInsets.Type.systemBars())
                previousWebView?.visibility = View.VISIBLE

                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                if (view == null || icon == null) return

                val tab = tabManager.getTabs().find { it.webView == view } ?: return

                tab.favicon = icon
                refreshTabBar()
            }

        }

        tabManager = TabManager(
            context = this,
            container = container,
            webViewClient = webViewClient,
            webChromeClient = webChromeClient
        )

        if (savedInstanceState == null) {
            tabManager.createTab("https://duckduckgo.com")
            // TODO: Refactor with observable or something like that
            refreshTabBar()

        }

        setupLongPressForCurrentWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {
                val webView = tabManager.getCurrentTab()?.webView
                if (webView != null && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        urlBar.setOnEditorActionListener { v, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = urlBar.text.toString()
                tabManager.getCurrentTab()?.webView?.loadUrl(
                    "https://duckduckgo.com/?q=${
                        URLEncoder.encode(
                            input,
                            "UTF-8"
                        )
                    }"
                )
                true
            } else false
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setupLongPressForCurrentWebView() {
        val webView = tabManager.getCurrentTab()?.webView ?: return

        webView.setOnLongClickListener { view ->
            val hit = (view as WebView).hitTestResult
            val type = hit.type
            val extra = hit.extra

            if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val url = extra ?: return@setOnLongClickListener true

                tabManager.createTab(url, switchTo = false)
                // TODO: Refactor with observable or something like that
                refreshTabBar()

                Toast.makeText(this, "Onglet ouvert en arrière-plan", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshTabBar() {
        val tabBar = findViewById<LinearLayout>(R.id.tab_bar)
        tabBar.removeAllViews()

        val tabs = tabManager.getTabs()

        if (tabs.size <= 1) {
            tabBar.visibility = View.GONE
            return
        } else {
            tabBar.visibility = View.VISIBLE
        }

        tabs.forEach { tab ->
            val tabFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(50, 50).apply {
                    marginStart = 8
                    marginEnd = 8
                }

                background = if (tab == tabManager.getCurrentTab()) {
                    ResourcesCompat.getDrawable(resources, R.drawable.tab_circle_active_bg, null)
                } else {
                    ResourcesCompat.getDrawable(resources, R.drawable.tab_circle_bg, null)
                }

                clipToOutline = true

                val icon = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        60, 60, Gravity.CENTER
                    )

                    scaleType = ImageView.ScaleType.CENTER_CROP

                    if (tab.favicon != null) setImageBitmap(tab.favicon)
                    else setImageResource(R.drawable.tab_circle_bg)
                }

                addView(icon)

                setOnClickListener {
                    tabManager.switchTo(tab)
                    refreshTabBar()
                    setupLongPressForCurrentWebView()
                }
            }
            tabBar.addView(tabFrame)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
//        webView.restoreState(savedInstanceState)
    }

}