package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {}
    val button = login<caret>

    fun f() = login.toString()
}

