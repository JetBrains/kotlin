package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*

val Activity.MyButton: org.my.cool.Button
    get() = findViewById(0) as org.my.cool.Button

val Fragment.MyButton: org.my.cool.Button
    get() = getView().findViewById(0) as org.my.cool.Button

