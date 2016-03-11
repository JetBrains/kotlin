package com.myapp

import android.view.View
import kotlinx.android.synthetic.main.layout.view.*

fun View.a() {
    val button = this.login<caret>
}

// EXIST: login
