package kotlinx.android.synthetic.main.test.view

import kotlin.internal.flexible.ft

val android.view.View.textView1: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.view.View.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

