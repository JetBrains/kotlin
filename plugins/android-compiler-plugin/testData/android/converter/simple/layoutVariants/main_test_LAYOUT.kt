package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.app.Fragment.button: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

