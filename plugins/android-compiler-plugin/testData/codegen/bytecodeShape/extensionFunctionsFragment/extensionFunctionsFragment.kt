package com.myapp

import android.app.Fragment
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyFragment : Fragment()

fun MyFragment.b() {
    val x = login
    val y = this.login
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL com/myapp/MyFragment\.getView
// 2 GETSTATIC com/myapp/R\$id\.login
// 2 INVOKEVIRTUAL com/myapp/MyFragment\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button
