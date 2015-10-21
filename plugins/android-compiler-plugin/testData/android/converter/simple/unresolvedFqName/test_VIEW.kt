package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyView", "kotlinx.android.synthetic.main.test.view.MyView"))
@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.view.View.MyView: ft<a.b.c, a.b.c?>
    get() = findViewById(0) as? a.b.c

