package org.my.cool

import android.widget.Button
import android.content.Context

class MyButton(ctx: Context): Button(ctx) {
    override fun toString(): String {return "MyButton"}
}
