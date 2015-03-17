package kotlinx.android.synthetic.layout1

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*

val Activity.frameLayout: FrameLayout
    get() = findViewById(0) as FrameLayout

val Fragment.frameLayout: FrameLayout
    get() = getView().findViewById(0) as FrameLayout

val Activity.passwordField: TextView
    get() = findViewById(0) as TextView

val Fragment.passwordField: TextView
    get() = getView().findViewById(0) as TextView

val Activity.passwordCaption: EditText
    get() = findViewById(0) as EditText

val Fragment.passwordCaption: EditText
    get() = getView().findViewById(0) as EditText

val Activity.loginButton: Button
    get() = findViewById(0) as Button

val Fragment.loginButton: Button
    get() = getView().findViewById(0) as Button

