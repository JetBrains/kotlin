package kotlinx.android.synthetic.main.test.view

import kotlin.internal.flexible.ft

val android.view.View.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.view.View.button2: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.view.View.button3: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

