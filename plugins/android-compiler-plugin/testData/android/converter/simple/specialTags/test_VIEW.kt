package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("includeTag", "kotlinx.android.synthetic.main.test.view.includeTag"))
val android.view.View.includeTag: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`fun`", "kotlinx.android.synthetic.main.test.view.`fun`"))
val android.view.View.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`set`", "kotlinx.android.synthetic.main.test.view.`set`"))
val android.view.View.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

