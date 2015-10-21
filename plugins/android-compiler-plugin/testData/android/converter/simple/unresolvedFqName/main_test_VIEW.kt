package kotlinx.android.synthetic.main.test.view

import kotlin.internal.flexible.ft

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.view.View.MyView: ft<a.b.c, a.b.c?>
    get() = findViewById(0) as? a.b.c

