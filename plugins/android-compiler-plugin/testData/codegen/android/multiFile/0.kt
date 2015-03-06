package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class R {
    class id {
        default object {
            val item_detail_container = 0
            val textView1 = 1
            val password = 2
            val textView2 = 3
            val passwordConfirmation = 4
            val login = 5
            val passwordField = 6
            val passwordCaption = 7
            val loginButton = 8
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

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.textView1 -> textViewWidget
            R.id.password -> editTextWidget
            R.id.login -> buttonWidget
            R.id.passwordField -> textViewWidget2
            R.id.passwordCaption -> editTextWidget2
            R.id.loginButton -> buttonWidget2
            else -> null
        }
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
