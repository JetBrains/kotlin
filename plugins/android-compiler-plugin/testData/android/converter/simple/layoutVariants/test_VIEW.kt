package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.view.button"))
val android.view.View.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

