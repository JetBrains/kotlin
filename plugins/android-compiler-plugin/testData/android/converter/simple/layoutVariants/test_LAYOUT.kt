package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.button"))
val android.app.Activity.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.button"))
val android.app.Fragment.button: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

