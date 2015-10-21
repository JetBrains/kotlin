package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.button"))
val android.app.Activity.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button", "kotlinx.android.synthetic.main.test.button"))
val android.app.Fragment.button: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button2", "kotlinx.android.synthetic.main.test.button2"))
val android.app.Activity.button2: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button2", "kotlinx.android.synthetic.main.test.button2"))
val android.app.Fragment.button2: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button3", "kotlinx.android.synthetic.main.test.button3"))
val android.app.Activity.button3: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("button3", "kotlinx.android.synthetic.main.test.button3"))
val android.app.Fragment.button3: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

