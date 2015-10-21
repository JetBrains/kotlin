package com.myapp

import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*

class MyFragment: Fragment() {
    val button = this.MyButton
    val button2 = MyButton
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL com/myapp/MyFragment\.getView
// 2 GETSTATIC com/myapp/R\$id\.MyButton
// 2 INVOKEVIRTUAL com/myapp/MyFragment\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button
