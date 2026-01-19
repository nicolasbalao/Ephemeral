package com.example.ephemeral

data class TabState(
    val id: Int,
    val url: String,
    val title: String?,
    val scrollY: Int = 0
)
