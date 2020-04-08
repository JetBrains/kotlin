// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.AddServiceToManifest
// NOT_AVAILABLE
package com.myapp

import android.app.Service
import android.content.Intent
import android.os.IBinder


class Test {
    inner class <caret>MyService : Service() {
        override fun onBind(intent: Intent?): IBinder {
            TODO("not implemented")
        }
    }
}