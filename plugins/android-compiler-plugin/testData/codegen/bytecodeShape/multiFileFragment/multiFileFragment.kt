package com.myapp

import android.app.Activity
import android.app.Fragment
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class MyActivity: Activity() {
    val button = this.login
}

class MyFragment: Fragment() {
    val button1 = this.loginButton
}

// 2 GETSTATIC
// 11 INVOKEVIRTUAL
// 4 CHECKCAST
// 4  _\$_findCachedViewById
// 2 findViewById
// 1 getView