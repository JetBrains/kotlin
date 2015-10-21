package kotlinx.android.synthetic.main.test

import kotlin.internal.flexible.ft

val android.app.Activity.includeTag: ft<android.view.View, android.view.View?>
    get() = findViewById(0)

val android.app.Fragment.includeTag: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

val android.support.v4.app.Fragment.includeTag: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0)

val android.app.Activity.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

val android.app.Fragment.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

val android.support.v4.app.Fragment.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

val android.support.v4.app.FragmentActivity.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getSupportFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

val android.app.Activity.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = findViewById(0) as? android.widget.TextView

val android.app.Fragment.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

val android.support.v4.app.Fragment.`fun`: ft<android.widget.TextView, android.widget.TextView?>
    get() = getView().findViewById(0) as? android.widget.TextView

val android.app.Activity.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = findViewById(0) as? android.widget.Button

val android.app.Fragment.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

val android.support.v4.app.Fragment.`set`: ft<android.widget.Button, android.widget.Button?>
    get() = getView().findViewById(0) as? android.widget.Button

