package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*

class MyActivity: Activity() {
    val button = this.login<caret>
    val button1 = this.loginButton
}

