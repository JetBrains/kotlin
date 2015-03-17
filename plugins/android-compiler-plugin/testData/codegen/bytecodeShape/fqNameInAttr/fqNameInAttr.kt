package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*

class MyActivity: Activity() {
    val button = this.MyButton
    val button2 = MyButton
}

// 2 GETSTATIC
// 6 INVOKEVIRTUAL
// 3 CHECKCAST
// 3  _\$_findCachedViewById
// 1 findViewById