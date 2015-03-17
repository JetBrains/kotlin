package android.app

import android.view.View
import android.content.Context

open class Activity: Context {
    open fun findViewById(id: Int): View? = null
}
