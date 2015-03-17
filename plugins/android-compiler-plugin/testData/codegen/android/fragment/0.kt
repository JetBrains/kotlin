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

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.login -> buttonWidget
            else -> null
        }
    }
}

class MyFragment(): Fragment() {
    val baseActivity = Activity()
    val baseView = BaseView(baseActivity)

    override fun getActivity(): Activity = baseActivity

    override fun getView(): View = baseView

    public fun box(): String {
        return if (login.toString() == "MyButton") "OK" else ""
    }
}

fun box(): String {
    return MyFragment().box()
}
