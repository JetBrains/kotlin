package android.app

import android.view.View

open class Dialog {
    open fun <T : View> findViewById(id: Int): T? = null
}
