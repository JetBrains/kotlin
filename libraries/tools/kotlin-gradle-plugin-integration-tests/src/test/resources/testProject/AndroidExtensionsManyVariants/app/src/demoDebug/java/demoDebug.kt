package org.example.manyvariants

import android.app.Activity
import kotlinx.android.synthetic.debug.activity_debug.*
import kotlinx.android.synthetic.demo.activity_demo.*
import kotlinx.android.synthetic.demoDebug.activity_demo_debug.*
import kotlinx.android.synthetic.main.activity_main.*

fun Activity.demoDebug() {
    viewMain
    viewDemo
    viewDebug
    viewDemoDebug
}
