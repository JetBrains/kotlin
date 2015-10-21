package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Activity.MyView: ft<a.b.c, a.b.c?>
    get() = findViewById(0) as? a.b.c

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Fragment.MyView: ft<a.b.c, a.b.c?>
    get() = getView().findViewById(0) as? a.b.c

