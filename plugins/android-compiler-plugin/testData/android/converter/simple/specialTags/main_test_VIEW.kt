package kotlinx.android.synthetic.main.test.view

import kotlin.internal.flexible.ft

val android.view.View.includeTag: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.view.View.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.view.View.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

