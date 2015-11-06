package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*

class MyActivity: Activity() {
    val button = this.<caret>login
    val button1 = this.loginButton
}

