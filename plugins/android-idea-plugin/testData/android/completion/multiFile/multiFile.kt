package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class MyActivity: Activity() {
    val button = log<caret>
}

// EXIST: login, loginButton
