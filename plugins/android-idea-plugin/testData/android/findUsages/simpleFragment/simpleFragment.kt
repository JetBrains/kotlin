package com.myapp

import android.app.Fragment
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyFragemnt : Fragment() {
    override fun onResume() {}
    val button = login<caret>

    fun f() = login.toString()
}

