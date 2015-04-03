package kotlinx.android.synthetic.test

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

val android.app.Activity.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = findViewById(0) as? FrameLayout

val android.app.Fragment.item_detail_container: ft<FrameLayout, FrameLayout?>
    get() = getView().findViewById(0) as? FrameLayout

val android.app.Activity.textView1: ft<TextView, TextView?>
    get() = findViewById(0) as? TextView

val android.app.Fragment.textView1: ft<TextView, TextView?>
    get() = getView().findViewById(0) as? TextView

val android.app.Activity.password: ft<EditText, EditText?>
    get() = findViewById(0) as? EditText

val android.app.Fragment.password: ft<EditText, EditText?>
    get() = getView().findViewById(0) as? EditText

val android.app.Activity.login: ft<Button, Button?>
    get() = findViewById(0) as? Button

val android.app.Fragment.login: ft<Button, Button?>
    get() = getView().findViewById(0) as? Button

