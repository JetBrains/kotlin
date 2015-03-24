package kotlinx.android.synthetic.layout

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

val android.app.Activity.textView1: ft<View, View?>
    get() = findViewById(0) : View

val android.app.Fragment.textView1: ft<View, View?>
    get() = getView().findViewById(0) : View

val android.app.Activity.textView2: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val android.app.Fragment.textView2: ft<TextView, TextView?>
    get() = getView().findViewById(0) as TextView

