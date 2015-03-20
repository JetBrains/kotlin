package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.button: ft<View, View?>
    get() = findViewById(0) : View

val Fragment.button: ft<View, View?>
    get() = getView().findViewById(0) : View

