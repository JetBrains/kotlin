package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class MyFragment: Fragment() {
    val button = log<caret>
}

// EXIST: login, loginButton
