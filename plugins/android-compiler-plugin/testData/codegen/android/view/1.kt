package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout.view.*

class R {
    class id {
        companion object {
            val container = 0
            val login = 1
        }
    }
}

class MyActivity(): Activity() {
    val containerWidget = object : FrameLayout(this) {
        val loginWidget = Button(this@MyActivity)
    }

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.container -> containerWidget
            else -> null
        }
    }

}

fun box(): String {
    return "OK"
}
