package kotlinx.android.synthetic.main.test1

import kotlin.internal.flexible.ft

val android.app.Activity.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

val android.app.Fragment.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = getView().findViewById(0) as? android.widget.FrameLayout

val android.app.Activity.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.app.Fragment.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

val android.app.Activity.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

val android.app.Fragment.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = getView().findViewById(0) as? android.widget.EditText

val android.app.Activity.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.app.Fragment.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

