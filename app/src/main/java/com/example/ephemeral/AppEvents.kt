package com.example.ephemeral

object AppEvents {
    private val listeners = mutableListOf<() -> Unit>()

    fun registerOnAppClosed(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun unregister(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyAppBackgrounded() {
        listeners.forEach { it.invoke() }
    }

}