package test

import android.app.Activity
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class R {
    class id {
        companion object {
            const val item_detail_container = 0
            const val textView1 = 1
            const val password = 2
            const val textView2 = 3
            const val passwordConfirmation = 4
            const val login = 5
            const val passwordField = 6
            const val passwordCaption = 7
            const val loginButton = 8
         }
    }
}

class MyActivity(): Activity() {
    val textViewWidget = TextView(this)
    val editTextWidget = EditText(this)
    val buttonWidget = Button(this)
    val textViewWidget2 = TextView(this)
    val editTextWidget2 = EditText(this)
    val buttonWidget2 = Button(this)

    override fun <T : View> findViewById(id: Int): T? {
        return when (id) {
            R.id.textView1 -> textViewWidget
            R.id.password -> editTextWidget
            R.id.login -> buttonWidget
            R.id.passwordField -> textViewWidget2
            R.id.passwordCaption -> editTextWidget2
            R.id.loginButton -> buttonWidget2
            else -> null
        } as T?
    }


    public fun box(): String{
        return if (textView1.toString() == "TextView" &&
                   password.toString() == "EditText" &&
                   login.toString() == "Button" &&
                   passwordField.toString() == "TextView" &&
                   passwordCaption.toString() == "EditText" &&
                   loginButton.toString() == "Button")
            "OK" else ""
    }
}

fun box(): String {
    return MyActivity().box()
}
