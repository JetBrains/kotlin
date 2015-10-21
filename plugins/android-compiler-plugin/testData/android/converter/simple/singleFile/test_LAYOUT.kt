package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("item_detail_container", "kotlinx.android.synthetic.main.test.item_detail_container"))
val android.app.Activity.item_detail_container: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("item_detail_container", "kotlinx.android.synthetic.main.test.item_detail_container"))
val android.app.Fragment.item_detail_container: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = getView().findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.textView1"))
val android.app.Activity.textView1: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.textView1"))
val android.app.Fragment.textView1: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("password", "kotlinx.android.synthetic.main.test.password"))
val android.app.Activity.password: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("password", "kotlinx.android.synthetic.main.test.password"))
val android.app.Fragment.password: ft<android.widget.EditText, android.widget.EditText?>
    get() = getView().findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("login", "kotlinx.android.synthetic.main.test.login"))
val android.app.Activity.login: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("login", "kotlinx.android.synthetic.main.test.login"))
val android.app.Fragment.login: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

