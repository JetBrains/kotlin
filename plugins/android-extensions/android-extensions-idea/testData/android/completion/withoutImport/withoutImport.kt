package com.myapp

import android.app.Activity

class MyActivity: Activity() {
    val button = this.login<caret>
}

// EXIST: { lookupString:"login", tailText: " from layout.xml for Activity (Android Extensions)", typeText:"Button!" }
