package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class R {
    class id {
        companion object {
            val item_detail_container = 0
            val textView1 = 1
            val password = 2
            val textView2 = 3
            val passwordConfirmation = 4
            val login = 5
        }
    }
}

class MyActivity(): Activity() {
    val textViewWidget = TextView(this)
    val editTextWidget = EditText(this)
    val buttonWidget = Button(this)

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.textView1 -> textViewWidget
            R.id.password -> editTextWidget
            R.id.login -> buttonWidget
            else -> null
        }
    }
}

fun box(): String {
    return "OK"
}
