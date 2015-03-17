package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class MyActivity: Activity() {
    val button = this.login
    val button1 = this.loginButton
}

// 2 GETSTATIC
// 6 INVOKEVIRTUAL
// 3 CHECKCAST
// 3  _\$_findCachedViewById
// 1 findViewById