package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.app.Fragment.button: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

val android.app.Activity.button2: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.app.Fragment.button2: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

val android.app.Activity.button3: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.app.Fragment.button3: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

