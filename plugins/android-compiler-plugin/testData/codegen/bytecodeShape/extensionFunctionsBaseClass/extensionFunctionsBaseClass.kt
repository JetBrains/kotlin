package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.layout.*

public class MyActivity : Activity() {

}

fun Activity.b() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC
// 6 INVOKEVIRTUAL
// 3 CHECKCAST
// 1  _\$_findCachedViewById
// 3 findViewById