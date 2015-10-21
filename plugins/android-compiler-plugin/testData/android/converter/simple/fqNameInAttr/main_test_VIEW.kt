package kotlinx.android.synthetic.main.test.view

import kotlin.internal.flexible.ft

val android.view.View.MyKeyboardView: ft<android.inputmethodservice.KeyboardView, android.inputmethodservice.KeyboardView?>
    get() = findViewById(0) as? android.inputmethodservice.KeyboardView

