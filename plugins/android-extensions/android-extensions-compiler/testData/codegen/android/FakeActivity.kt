package android.app

import android.view.View
import android.content.Context

open class Activity: Context {
    open fun <T : View> findViewById(id: Int): T? = null
}
