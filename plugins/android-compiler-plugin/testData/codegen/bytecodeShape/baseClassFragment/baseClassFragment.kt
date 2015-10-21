package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*

fun Fragment.a() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC com/myapp/R\$id\.login
// 2 CHECKCAST android/widget/Button
// 2 INVOKEVIRTUAL android/app/Fragment\.getView
// 2 INVOKEVIRTUAL android/view/View\.findViewById
// 0  _\$_findCachedViewById