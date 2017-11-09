package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*

class MyActivity: Activity() {
    val button = this.MyBu<caret>
}

// EXIST: { lookupString:"MyButton", tailText: " from layout.xml for Activity (Android Extensions)", typeText:"View!" }
