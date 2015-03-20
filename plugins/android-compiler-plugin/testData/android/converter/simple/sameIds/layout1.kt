package kotlinx.android.synthetic.layout.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val View.textView1: ft<View, View?>
    get() = findViewById(0) : View

val View.textView2: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

