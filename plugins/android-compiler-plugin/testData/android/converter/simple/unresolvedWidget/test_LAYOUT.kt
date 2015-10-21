package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.MyKeyboardView"))
@kotlin.internal.flexible.InvalidWidgetType("KeyboardView")
val android.app.Activity.MyKeyboardView: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyKeyboardView", "kotlinx.android.synthetic.main.test.MyKeyboardView"))
@kotlin.internal.flexible.InvalidWidgetType("KeyboardView")
val android.app.Fragment.MyKeyboardView: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

