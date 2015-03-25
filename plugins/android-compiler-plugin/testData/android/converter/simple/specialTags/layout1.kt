package kotlinx.android.synthetic.layout.view

import android.app.Activity
import android.app.Fragment
import android.view.View
import android.widget.*
import kotlin.internal.flexible.ft

val View.includeTag: ft<View, View?>
    get() = findViewById(0) : View

val View.fragmentTag: ft<View, View?>
    get() = findViewById(0) : View

