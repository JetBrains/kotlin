package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*

class MyFragment: Fragment() {
    val button = this.MyBu<caret>
}

// EXIST: { lookupString:"MyButton", tailText: " from layout.xml for Fragment (Android Extensions)", typeText:"View!" }
