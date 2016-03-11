package test

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*

class MyActivity: Activity() {
    val button = this.MyButton<caret>
}

