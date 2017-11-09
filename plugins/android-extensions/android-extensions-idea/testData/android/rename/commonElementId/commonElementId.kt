package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

// KT-16132 Renaming property provided by kotlinx leads to renaming another members

object Loginer {
    fun login() { }
}

public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        <caret>login.setOnClickListener {
            Loginer.login()
        }
    }
}
