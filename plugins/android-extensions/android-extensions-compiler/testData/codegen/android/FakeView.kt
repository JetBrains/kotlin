package android.view

import android.content.Context

open class View(ctx: Context) {
    open fun <T : View> findViewById(id: Int): T? = null
}
