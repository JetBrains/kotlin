package org.example.kotlin.butterknife

import android.app.Application
import butterknife.ButterKnife

class SimpleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ButterKnife.setDebug(BuildConfig.DEBUG)
    }
}
