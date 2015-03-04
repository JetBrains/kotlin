package kotlinx.android.synthetic.layout1.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*

val View.frameLayout: FrameLayout
    get() = findViewById(0) as FrameLayout

val View.passwordField: TextView
    get() = findViewById(0) as TextView

val View.passwordCaption: EditText
    get() = findViewById(0) as EditText

val View.loginButton: Button
    get() = findViewById(0) as Button

