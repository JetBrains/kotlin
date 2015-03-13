package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.layout.*

fun Fragment.a() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC
// 4 INVOKEVIRTUAL
// 2 CHECKCAST
// 0  _\$_findCachedViewById
// 2 findViewById
// 2 getView