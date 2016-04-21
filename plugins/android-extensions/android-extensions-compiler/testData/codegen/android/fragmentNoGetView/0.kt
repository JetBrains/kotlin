package test

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.view.View
import android.widget.*
import org.my.cool.MyButton
import kotlinx.android.synthetic.main.layout.*

class R {
    class id {
        companion object {
            const val login = 5
        }
    }
}

class MyFragment(): Fragment() {
    val baseActivity = Activity()

    override fun getActivity(): Activity = baseActivity

    override fun getView(): View? = null

    public fun box(): String {
        val button = login
        return if (button == null) "OK" else "Button is not null: $button"
    }
}

fun box(): String {
    return MyFragment().box()
}
