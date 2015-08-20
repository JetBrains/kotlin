package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.view.View.MyView: ft<android.view.View, android.view.View?>
    get() = findViewById(0) as? android.view.View

