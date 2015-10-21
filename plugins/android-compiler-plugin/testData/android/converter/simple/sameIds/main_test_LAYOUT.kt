package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.textView1: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.app.Fragment.textView1: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

val android.app.Activity.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.app.Fragment.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

