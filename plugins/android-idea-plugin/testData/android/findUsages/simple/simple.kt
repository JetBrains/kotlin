package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File


public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {}
    val button = login<caret>

    fun f() = login.toString()
}

