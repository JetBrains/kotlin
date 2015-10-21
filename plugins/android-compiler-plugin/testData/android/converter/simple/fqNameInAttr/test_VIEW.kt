package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.view.MyKeyboardView"))
val android.view.View.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = findViewById(0) as? android.inputmethodservice.KeyboardView

