package com.example.ephemeral

import android.graphics.Bitmap
import android.webkit.WebView

data class Tab(
    var id: Int,
    val webView: WebView,
    var favicon: Bitmap? = null,
    var url: String? = null
)
