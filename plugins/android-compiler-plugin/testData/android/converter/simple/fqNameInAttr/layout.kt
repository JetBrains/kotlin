package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.MyButton: ft<org.my.cool.Button, org.my.cool.Button?>
    get() = findViewById(0) as org.my.cool.Button

val Fragment.MyButton: ft<org.my.cool.Button, org.my.cool.Button?>
    get() = getView().findViewById(0) as org.my.cool.Button

