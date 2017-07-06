package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*

class MyFragment: Fragment() {
    val button = this.login<caret>
}

// EXIST: { lookupString:"login", tailText: " from layout.xml for Fragment (Android Extensions)", typeText:"Button!" }
