package com.myapp

import android.view.View
import kotlinx.android.synthetic.main.layout.view.*

fun View.a() {
    val button = this.login<caret>
}

// EXIST: { lookupString:"login", tailText: " from layout.xml for View (Android Extensions)", typeText:"Button!" }
