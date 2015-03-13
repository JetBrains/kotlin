package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class MyFragment: Fragment() {
    val button = log<caret>
}

// EXIST: login, loginButton
