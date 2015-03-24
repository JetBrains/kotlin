package kotlinx.android.synthetic.layout.view

import android.app.*
import android.view.*
import android.widget.*
import android.webkit.*
import android.inputmethodservice.*
import android.opengl.*
import android.appwidget.*
import android.support.v4.app.*
import android.support.v4.view.*
import android.support.v4.widget.*
import kotlin.internal.flexible.ft

val android.view.View.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as FrameLayout

val android.view.View.textView1: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val android.view.View.password: ft<EditText, EditText?>
    get() = findViewById(0) as EditText

val android.view.View.login: ft<Button, Button?>
    get() = findViewById(0) as Button

