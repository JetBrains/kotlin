package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.includeTag: ft<View, View?>
    get() = findViewById(0) : View

val Fragment.includeTag: ft<View, View?>
    get() = getView().findViewById(0) : View

val Activity.fragmentTag: ft<View, View?>
    get() = findViewById(0) : View

val Fragment.fragmentTag: ft<View, View?>
    get() = getView().findViewById(0) : View

val Activity.`fun`: ft<TextView, TextView?>
    get() = findViewById(0) as TextView

val Fragment.`fun`: ft<TextView, TextView?>
    get() = getView().findViewById(0) as TextView

