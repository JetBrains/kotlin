package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.view.button"))
val android.view.View.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button2", "kotlinx.android.synthetic.main.test.view.button2"))
val android.view.View.button2: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button3", "kotlinx.android.synthetic.main.test.view.button3"))
val android.view.View.button3: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

