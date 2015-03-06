package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*

fun Activity.a() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC
// 2 INVOKEVIRTUAL
// 2 CHECKCAST
// 0  _\$_findCachedViewById
// 2 findViewById