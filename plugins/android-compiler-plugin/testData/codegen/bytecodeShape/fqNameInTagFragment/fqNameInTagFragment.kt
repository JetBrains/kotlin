package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.layout.*

class MyFragment: Fragment() {
    val button = this.MyButton
    val button2 = MyButton
}

// 2 GETSTATIC
// 7 INVOKEVIRTUAL
// 3 CHECKCAST
// 3  _\$_findCachedViewById
// 1 findViewById
// 1 getView