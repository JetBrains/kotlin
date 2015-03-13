package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*

val Activity.item_detail_container: FrameLayout
    get() = findViewById(0) as FrameLayout

val Fragment.item_detail_container: FrameLayout
    get() = getView().findViewById(0) as FrameLayout

val Activity.textView1: TextView
    get() = findViewById(0) as TextView

val Fragment.textView1: TextView
    get() = getView().findViewById(0) as TextView

val Activity.password: EditText
    get() = findViewById(0) as EditText

val Fragment.password: EditText
    get() = getView().findViewById(0) as EditText

val Activity.login: Button
    get() = findViewById(0) as Button

val Fragment.login: Button
    get() = getView().findViewById(0) as Button

