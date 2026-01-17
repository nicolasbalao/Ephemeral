package com.example.ephemeral

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

class TabManager(
    private val context: Context,
    private val container: FrameLayout,
    private val webViewClient: WebViewClient,
    private val webChromeClient: WebChromeClient
) {

    private val tabs = mutableListOf<Tab>()
    private var currentTab: Tab? = null
    private var nextId = 0


    @RequiresApi(Build.VERSION_CODES.O)
    fun newTab(): Tab {
        return createTab("https://duckduckgo.com", switchTo = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createTab(url: String, switchTo: Boolean = true): Tab {
        val webView: WebView = createWebView();
        val tab = Tab(nextId++, webView)

        tabs.add(tab)
        container.addView(webView)

        webView.webViewClient = this.webViewClient
        webView.webChromeClient = this.webChromeClient

        webView.loadUrl(url)

        if (switchTo) {
            switchTo(tab)
        } else {
            webView.visibility = View.GONE
        }
        return tab
    }

    fun switchTo(tab: Tab) {
        currentTab?.webView?.visibility = View.GONE
        tab.webView.visibility = View.VISIBLE
        currentTab = tab
    }

    fun getCurrentTab(): Tab? = currentTab
    fun getTabs(): List<Tab> = tabs

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createWebView(): WebView {
        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true;
            settings.domStorageEnabled = true
            visibility = View.GONE
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun closeCurrentTab() {
        val tab = currentTab ?: return

        val index = tabs.indexOf(tab)

        container.removeView(tab.webView)
        tab.webView.destroy()
        tabs.remove(tab)

        val nextTab = when {
            tabs.isEmpty() -> {
                newTab()
            }

            index > 0 -> tabs[index - 1]
            else -> tabs.first()
        }

        switchTo(nextTab)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun closeTab(tab: Tab) {
        container.removeView(tab.webView)
        tab.webView.destroy()

        if (tab == currentTab) {
            val index = tabs.indexOf(tab)
            val nextTab = when {
                tabs.isEmpty() -> {
                    newTab()
                }

                index > 0 -> tabs[index - 1]
                else -> tabs.first()
            }

            switchTo(nextTab)
        }
        tabs.remove(tab)

    }


}