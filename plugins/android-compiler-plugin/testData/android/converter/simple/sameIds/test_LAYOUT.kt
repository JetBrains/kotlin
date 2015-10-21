package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.textView1"))
val android.app.Activity.textView1: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView1", "kotlinx.android.synthetic.main.test.textView1"))
val android.app.Fragment.textView1: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView2", "kotlinx.android.synthetic.main.test.textView2"))
val android.app.Activity.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("textView2", "kotlinx.android.synthetic.main.test.textView2"))
val android.app.Fragment.textView2: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

