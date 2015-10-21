package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyView", "kotlinx.android.synthetic.main.test.MyView"))
@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Activity.MyView: ft<a.b.c, a.b.c?>
    get() = findViewById(0) as? a.b.c

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("MyView", "kotlinx.android.synthetic.main.test.MyView"))
@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Fragment.MyView: ft<a.b.c, a.b.c?>
    get() = getView().findViewById(0) as? a.b.c

