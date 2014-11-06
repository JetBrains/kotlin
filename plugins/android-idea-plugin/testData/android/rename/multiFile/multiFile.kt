package com.myapp

import android.app.Activity


class MyActivity: Activity() {
    val button = this.login<caret>
    val button1 = this.loginButton
}

