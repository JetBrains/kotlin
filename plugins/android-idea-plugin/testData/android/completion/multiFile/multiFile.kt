package com.myapp

import android.app.Activity


class MyActivity: Activity() {
    val button = this.log<caret>
}

// EXIST: login, loginButton
