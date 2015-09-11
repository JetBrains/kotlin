package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Activity.MyView: ft<android.view.View, android.view.View?>
    get() = findViewById(0) as? android.view.View

@kotlin.internal.flexible.InvalidWidgetType("a.b.c")
val android.app.Fragment.MyView: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0) as? android.view.View

