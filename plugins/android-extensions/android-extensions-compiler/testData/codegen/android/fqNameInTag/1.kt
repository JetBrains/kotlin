package test

import android.app.Activity
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

class MyActivity(): Activity() {
    val buttonWidget = MyButton(this)

    override fun <T : View> findViewById(id: Int): T? {
        return when (id) {
            R.id.login -> buttonWidget as T
            else -> null
        }
    }
}

fun box(): String {
    return "OK"
}
