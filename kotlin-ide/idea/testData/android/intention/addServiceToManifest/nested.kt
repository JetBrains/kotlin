// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.AddServiceToManifest
// CHECK_MANIFEST
package com.myapp

import android.app.Service
import android.content.Intent
import android.os.IBinder


class Test {
    class <caret>MyService : Service() {
        override fun onBind(intent: Intent?): IBinder {
            TODO("not implemented")
        }
    }
}