package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = findViewById(0) as? android.inputmethodservice.KeyboardView

val android.app.Fragment.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = getView().findViewById(0) as? android.inputmethodservice.KeyboardView

