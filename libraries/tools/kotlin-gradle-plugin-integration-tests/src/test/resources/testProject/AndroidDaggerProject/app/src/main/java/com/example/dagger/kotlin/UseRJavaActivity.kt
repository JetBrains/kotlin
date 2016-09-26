package com.example.dagger.kotlin

import android.app.Activity

class UseRJavaActivity : Activity() {
    fun useRJava() {
        val app_name = getResources().getString(R.string.app_name)
    }
}
