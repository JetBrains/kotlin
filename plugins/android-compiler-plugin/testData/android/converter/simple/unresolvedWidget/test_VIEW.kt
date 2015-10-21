package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.view.MyKeyboardView"))
@kotlin.internal.flexible.InvalidWidgetType("KeyboardView")
val android.view.View.MyKeyboardView: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

