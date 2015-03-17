package com.myapp

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.view.View
import android.widget.*
import org.my.cool.MyButton
import kotlinx.android.synthetic.layout.*

class R {
    class id {
        class object {
            val login = 5
        }
    }
}

class BaseView(ctx: Context) : View(ctx) {
    val buttonWidget = MyButton(ctx)
}

class MyFragment(): Fragment() {
    val baseActivity = Activity()
    val baseView = BaseView(baseActivity)

    override fun getView(): View = baseView
}

fun box(): String {
    return "OK"
}
