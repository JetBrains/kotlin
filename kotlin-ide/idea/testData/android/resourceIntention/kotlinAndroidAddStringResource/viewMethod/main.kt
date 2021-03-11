package com.myapp

import android.content.Context
import android.view.View

class DemoView constructor(context: Context): View(context) {
    fun test() {
        val b = "some <caret>string"
    }
}