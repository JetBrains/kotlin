package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("includeTag", "kotlinx.android.synthetic.main.test.includeTag"))
val android.app.Activity.includeTag: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("includeTag", "kotlinx.android.synthetic.main.test.includeTag"))
val android.app.Fragment.includeTag: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("includeTag", "kotlinx.android.synthetic.main.test.includeTag"))
val android.support.v4.app.Fragment.includeTag: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("fragmentTag", "kotlinx.android.synthetic.main.test.fragmentTag"))
val android.app.Activity.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("fragmentTag", "kotlinx.android.synthetic.main.test.fragmentTag"))
val android.app.Fragment.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("fragmentTag", "kotlinx.android.synthetic.main.test.fragmentTag"))
val android.support.v4.app.Fragment.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("fragmentTag", "kotlinx.android.synthetic.main.test.fragmentTag"))
val android.support.v4.app.FragmentActivity.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getSupportFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`fun`", "kotlinx.android.synthetic.main.test.`fun`"))
val android.app.Activity.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`fun`", "kotlinx.android.synthetic.main.test.`fun`"))
val android.app.Fragment.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`fun`", "kotlinx.android.synthetic.main.test.`fun`"))
val android.support.v4.app.Fragment.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`set`", "kotlinx.android.synthetic.main.test.`set`"))
val android.app.Activity.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`set`", "kotlinx.android.synthetic.main.test.`set`"))
val android.app.Fragment.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

@Deprecated("Use the property from the 'main' variant instead", ReplaceWith("`set`", "kotlinx.android.synthetic.main.test.`set`"))
val android.support.v4.app.Fragment.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

