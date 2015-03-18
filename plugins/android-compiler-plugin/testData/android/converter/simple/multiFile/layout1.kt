package kotlinx.android.synthetic.layout.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val View.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as FrameLayout

val View.textView1: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val View.password: ft<EditText, EditText?>
    get() = findViewById(0) as EditText

val View.login: ft<Button, Button?>
    get() = findViewById(0) as Button

