package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as FrameLayout

val Fragment.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = getView().findViewById(0) as FrameLayout

val Activity.textView1: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val Fragment.textView1: ft<TextView, TextView?>
    get() = getView().findViewById(0) as TextView

val Activity.password: ft<EditText, EditText?>
    get() = findViewById(0) as EditText

val Fragment.password: ft<EditText, EditText?>
    get() = getView().findViewById(0) as EditText

val Activity.login: ft<Button, Button?>
    get() = findViewById(0) as Button

val Fragment.login: ft<Button, Button?>
    get() = getView().findViewById(0) as Button

