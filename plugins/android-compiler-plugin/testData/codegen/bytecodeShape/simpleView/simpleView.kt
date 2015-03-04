package com.myapp

import android.view.View
import android.app.Activity
import kotlinx.android.synthetic.layout.view.*

public class MyActivity : Activity() {
    { View(this).login }
}

// 1 GETSTATIC
// 4 INVOKEVIRTUAL
// 3 CHECKCAST
// 1 _\$_findCachedViewById
// 2 findViewById