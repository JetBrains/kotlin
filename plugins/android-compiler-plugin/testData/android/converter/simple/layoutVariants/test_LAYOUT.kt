package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

val android.app.Activity.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0) as? android.view.View

val android.app.Fragment.button: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0) as? android.view.View

