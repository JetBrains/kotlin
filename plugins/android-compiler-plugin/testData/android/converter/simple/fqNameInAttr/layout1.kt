package kotlinx.android.synthetic.layout.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val View.MyButton: ft<org.my.cool.Button, org.my.cool.Button?>
    get() = findViewById(0) as org.my.cool.Button

