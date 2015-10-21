package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.MyKeyboardView"))
val android.app.Activity.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = findViewById(0) as? android.inputmethodservice.KeyboardView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.MyKeyboardView"))
val android.app.Fragment.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = getView().findViewById(0) as? android.inputmethodservice.KeyboardView

