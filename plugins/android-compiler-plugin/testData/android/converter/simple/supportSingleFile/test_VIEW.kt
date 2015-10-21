package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("item_detail_container", "kotlinx.android.synthetic.main.test.view.item_detail_container"))
val android.view.View.item_detail_container: ft<android.widget.FrameLayout, android.widget.FrameLayout?>
    get() = findViewById(0) as? android.widget.FrameLayout

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.view.textView1"))
val android.view.View.textView1: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("password", "kotlinx.android.synthetic.main.test.view.password"))
val android.view.View.password: ft<android.widget.EditText, android.widget.EditText?>
    get() = findViewById(0) as? android.widget.EditText

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("login", "kotlinx.android.synthetic.main.test.view.login"))
val android.view.View.login: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

