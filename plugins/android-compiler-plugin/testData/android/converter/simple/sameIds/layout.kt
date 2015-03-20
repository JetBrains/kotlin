package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.textView1: ft<View, View?>
    get() = findViewById(0) : View

val Fragment.textView1: ft<View, View?>
    get() = getView().findViewById(0) : View

val Activity.textView2: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val Fragment.textView2: ft<TextView, TextView?>
    get() = getView().findViewById(0) as TextView

