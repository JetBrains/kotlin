package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {}
    val button = <caret>login
}

