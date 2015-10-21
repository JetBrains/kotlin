package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.view.textView1"))
val android.view.View.textView1: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView2", "kotlinx.android.synthetic.main.test.view.textView2"))
val android.view.View.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

