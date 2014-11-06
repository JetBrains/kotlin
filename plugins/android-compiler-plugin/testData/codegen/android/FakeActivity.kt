package android.app

import android.view.View
import android.content.Context

abstract class Activity: Context {
    abstract fun findViewById(id: Int): View?
}
