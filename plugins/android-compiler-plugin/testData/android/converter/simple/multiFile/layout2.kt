package kotlinx.android.synthetic.layout1

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.frameLayout: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as FrameLayout

val Fragment.frameLayout: ft<FrameLayout, FrameLayout?>
    get() = getView().findViewById(0) as FrameLayout

val Activity.passwordField: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val Fragment.passwordField: ft<TextView, TextView?>
    get() = getView().findViewById(0) as TextView

val Activity.passwordCaption: ft<EditText, EditText?>
    get() = findViewById(0) as EditText

val Fragment.passwordCaption: ft<EditText, EditText?>
    get() = getView().findViewById(0) as EditText

val Activity.loginButton: ft<Button, Button?>
    get() = findViewById(0) as Button

val Fragment.loginButton: ft<Button, Button?>
    get() = getView().findViewById(0) as Button

