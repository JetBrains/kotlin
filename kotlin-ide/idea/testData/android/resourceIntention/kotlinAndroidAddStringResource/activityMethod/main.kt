package com.myapp

import android.app.Activity

class MyActivity: Activity() {
    fun foo() {
        val a = "some <caret>string"
    }
}