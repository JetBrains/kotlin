package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class MyActivity: Activity() {
    val button = log<caret>
}

// EXIST: { lookupString:"login", tailText: " from layout.xml for Activity (Android Extensions)", typeText:"Button!" }
// EXIST: { lookupString:"loginButton", tailText: " from layout1.xml for Activity (Android Extensions)", typeText:"Button!" }
