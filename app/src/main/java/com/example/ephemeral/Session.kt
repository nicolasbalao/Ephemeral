package com.example.ephemeral

import kotlin.uuid.ExperimentalUuidApi

data class Session @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String,
    val name: String?,
    val createdAt: Long,
    var tabs: List<TabState>,
    val activeTabId: Int
)
