// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.AddBroadcastReceiverToManifest
// NOT_AVAILABLE
package com.myapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        <caret>
    }
}