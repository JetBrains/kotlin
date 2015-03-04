package kotlinx.android.synthetic.layout.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*

val View.item_detail_container: FrameLayout
    get() = findViewById(0) as FrameLayout

val View.textView1: TextView
    get() = findViewById(0) as TextView

val View.password: EditText
    get() = findViewById(0) as EditText

val View.login: Button
    get() = findViewById(0) as Button

