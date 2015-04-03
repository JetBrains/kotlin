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

val android.view.View.MyButton: ft<org.my.cool.Button, org.my.cool.Button?>
    get() = findViewById(0) as? org.my.cool.Button

