package kotlinx.android.synthetic.layout

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val Activity.includeTag: ft<View, View?>
    get() = findViewById(0) as View

val Fragment.includeTag: ft<View, View?>
    get() = getView().findViewById(0) as View

val Activity.mergeTag: ft<View, View?>
    get() = findViewById(0) as View

val Fragment.mergeTag: ft<View, View?>
    get() = getView().findViewById(0) as View

val Activity.fragmentTag: ft<View, View?>
    get() = findViewById(0) as View

val Fragment.fragmentTag: ft<View, View?>
    get() = getView().findViewById(0) as View

