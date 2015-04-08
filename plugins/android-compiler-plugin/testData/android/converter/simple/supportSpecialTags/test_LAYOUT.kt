package kotlinx.android.synthetic.test

import android.app.*
import android.view.*
import android.widget.*
import android.webkit.*
import android.inputmethodservice.*
import android.opengl.*
import android.appwidget.*
import android.support.v4.app.*
import android.support.v4.view.*
import android.support.v4.widget.*
import kotlin.internal.flexible.ft

val android.app.Activity.includeTag: ft<View, View?>
    get() = findViewById(0)

val android.app.Fragment.includeTag: ft<View, View?>
    get() = getView().findViewById(0)

val android.support.v4.app.Fragment.includeTag: ft<View, View?>
    get() = getView().findViewById(0)

val android.app.Activity.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

val android.app.Fragment.fragmentTag: ft<android.app.Fragment, android.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.app.Fragment

val android.support.v4.app.Fragment.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

val android.support.v4.app.FragmentActivity.fragmentTag: ft<android.support.v4.app.Fragment, android.support.v4.app.Fragment?>
    get() = getSupportFragmentManager().findFragmentById(0) as? android.support.v4.app.Fragment

val android.app.Activity.`fun`: ft<TextView, TextView?>
    get() = findViewById(0) as? TextView

val android.app.Fragment.`fun`: ft<TextView, TextView?>
    get() = getView().findViewById(0) as? TextView

val android.support.v4.app.Fragment.`fun`: ft<TextView, TextView?>
    get() = getView().findViewById(0) as? TextView

val android.app.Activity.`set`: ft<Button, Button?>
    get() = findViewById(0) as? Button

val android.app.Fragment.`set`: ft<Button, Button?>
    get() = getView().findViewById(0) as? Button

val android.support.v4.app.Fragment.`set`: ft<Button, Button?>
    get() = getView().findViewById(0) as? Button

