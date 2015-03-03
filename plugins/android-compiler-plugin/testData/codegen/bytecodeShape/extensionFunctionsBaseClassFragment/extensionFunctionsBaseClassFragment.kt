package com.myapp

import android.app.Fragment
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.layout.*

public class MyFragment : Fragment() {

}

fun Fragment.b() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC
// 8 INVOKEVIRTUAL
// 3 CHECKCAST
// 1  _\$_findCachedViewById
// 3 findViewById
// 3 getView