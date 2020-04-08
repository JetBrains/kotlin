package com.myapp

import android.app.Activity
import android.content.Context

class MyActivity: Activity() {
    object A {
        fun doSomething(context: Context) {
            "some <caret>string"
        }
    }
}