package test

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.view.View
import android.widget.*
import org.my.cool.MyButton
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.clearFindViewByIdCache

class R {
    class id {
        companion object {
            const val login = 5
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
        val result = if (login.toString() == "MyButton") "OK" else ""
        clearFindViewByIdCache()
        return result
    }
}

fun box(): String {
    return MyFragment().box()
}
