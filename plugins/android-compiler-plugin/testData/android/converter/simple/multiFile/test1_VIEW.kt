package kotlinx.android.synthetic.test1.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("frameLayout", "kotlinx.android.synthetic.main.test1.view.frameLayout"))
val android.view.View.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordField", "kotlinx.android.synthetic.main.test1.view.passwordField"))
val android.view.View.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordCaption", "kotlinx.android.synthetic.main.test1.view.passwordCaption"))
val android.view.View.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("loginButton", "kotlinx.android.synthetic.main.test1.view.loginButton"))
val android.view.View.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

