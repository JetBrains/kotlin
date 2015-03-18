package kotlinx.android.synthetic.layout1.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val View.frameLayout: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as FrameLayout

val View.passwordField: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val View.passwordCaption: ft<EditText, EditText?>
    get() = findViewById(0) as EditText

val View.loginButton: ft<Button, Button?>
    get() = findViewById(0) as Button

