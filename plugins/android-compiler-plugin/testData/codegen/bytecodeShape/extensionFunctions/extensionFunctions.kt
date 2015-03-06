package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.layout.*

public class MyActivity : Activity() {

}

fun MyActivity.b() {
    val x = login
    val y = this.login
}

// 2 GETSTATIC
// 5 INVOKEVIRTUAL
// 3 CHECKCAST
// 3  _\$_findCachedViewById
// 1 findViewById