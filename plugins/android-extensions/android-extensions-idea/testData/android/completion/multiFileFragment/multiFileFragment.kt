package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class MyFragment: Fragment() {
    val button = log<caret>
}

// EXIST: { lookupString:"login", tailText: " from layout.xml for Fragment (Android Extensions)", typeText:"Button!" }
// EXIST: { lookupString:"loginButton", tailText: " from layout1.xml for Fragment (Android Extensions)", typeText:"Button!" }