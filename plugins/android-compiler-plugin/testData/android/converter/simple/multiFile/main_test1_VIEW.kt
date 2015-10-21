package kotlinx.android.synthetic.main.test1.view

import kotlin.internal.flexible.ft

val android.view.View.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

val android.view.View.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.view.View.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

val android.view.View.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

