package kotlinx.android.synthetic.test.view

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

val android.view.View.button: ft<View, View?>
    get() = findViewById(0)

val android.view.View.button2: ft<Button, Button?>
    get() = findViewById(0) as? Button

val android.view.View.button3: ft<Button, Button?>
    get() = findViewById(0) as? Button

