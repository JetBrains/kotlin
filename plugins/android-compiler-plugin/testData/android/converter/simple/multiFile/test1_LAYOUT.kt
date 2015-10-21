package kotlinx.android.synthetic.test1

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("frameLayout", "kotlinx.android.synthetic.main.test1.frameLayout"))
val android.app.Activity.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("frameLayout", "kotlinx.android.synthetic.main.test1.frameLayout"))
val android.app.Fragment.frameLayout: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = getView().findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordField", "kotlinx.android.synthetic.main.test1.passwordField"))
val android.app.Activity.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordField", "kotlinx.android.synthetic.main.test1.passwordField"))
val android.app.Fragment.passwordField: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordCaption", "kotlinx.android.synthetic.main.test1.passwordCaption"))
val android.app.Activity.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("passwordCaption", "kotlinx.android.synthetic.main.test1.passwordCaption"))
val android.app.Fragment.passwordCaption: ft<android.widget.EditText, android.widget.EditText?>
    get() = getView().findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("loginButton", "kotlinx.android.synthetic.main.test1.loginButton"))
val android.app.Activity.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("loginButton", "kotlinx.android.synthetic.main.test1.loginButton"))
val android.app.Fragment.loginButton: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

