package com.example.ephemeral

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner


class App : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        AppEvents.notifyAppBackgrounded()
    }


}