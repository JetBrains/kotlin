package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.item_detail_container: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

val android.app.Fragment.item_detail_container: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = getView().findViewById(0) as? android.widget.FrameLayout

val android.app.Activity.textView1: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.app.Fragment.textView1: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

val android.app.Activity.password: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

val android.app.Fragment.password: ft<android.widget.EditText, android.widget.EditText?>
    get() = getView().findViewById(0) as? android.widget.EditText

val android.app.Activity.login: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.app.Fragment.login: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

