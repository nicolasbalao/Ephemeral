package com.example.ephemeral

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
    private val onAppClosed = {
        saveCurrentSession()
        SessionManager(this).currentSession = null
        tabManager.defaultTab()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val sessionPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val sessionId =
                    result.data?.getStringExtra("session_id") ?: return@registerForActivityResult
                val session =
                    SessionManager(this).getSession(sessionId) ?: return@registerForActivityResult
                SessionManager(this).currentSession = session
                tabManager.restoreSession(session)
            }
        }

    companion object {
        const val BASE_URL = "https://search.brave.com"
    }

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
                val tab = tabManager.getCurrentTab() ?: return
                if (tab.webView == view) {
                    tab.url = url
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // Don open new tab for the search result
                if (url.contains(BASE_URL)) {
                    return false
                }

                if (!request.isForMainFrame) return false

                // 🚫 Redirect ou navigation automatique → même tab
                if (!request.hasGesture()) {
                    return false
                }

                // 🧼 Premier load du tab → même tab
                if (view?.url == null) {
                    return false
                }

                // ✅ Clic utilisateur → nouvel onglet
                tabManager.createTab(url, switchTo = true)
                return true

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

        val homeBtn = findViewById<ImageButton>(R.id.home_btn)

        homeBtn.setOnClickListener {
            val currentTab = tabManager.getCurrentTab()
            currentTab?.webView?.loadUrl(BASE_URL)
        }

        if (savedInstanceState == null) {
            tabManager.createTab(BASE_URL)
            // TODO: Refactor with observable or something like that
            refreshTabBar()

        }

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
                    "${BASE_URL}/search?q=${
                        URLEncoder.encode(
                            input,
                            "UTF-8"
                        )
                    }"
                )
                true
            } else false
        }

        val newTabButton = findViewById<ImageButton>(R.id.new_tab_btn)


        newTabButton.setOnClickListener {
            tabManager.newTab()
            refreshTabBar()
        }

        newTabButton.setOnLongClickListener {
            sessionPicker.launch(Intent(this, SessionsActivity::class.java))
            true
        }

        AppEvents.registerOnAppClosed(onAppClosed)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        AppEvents.unregister(onAppClosed)
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
                layoutParams = LinearLayout.LayoutParams(90, 90).apply {
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
                }

                setOnLongClickListener {
                    tabManager.closeTab(tab)
                    refreshTabBar()
                    true
                }
            }
            tabBar.addView(tabFrame)
        }

    }


    private fun saveCurrentSession() {
        val session = tabManager.exportSession(SessionManager(this).currentSession)
        SessionManager(this).saveSession(session)
    }


}