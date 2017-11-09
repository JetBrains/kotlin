package test

import android.view.View
import android.app.Activity
import android.content.Context
import kotlinx.android.synthetic.main.layout.view.*
import kotlinx.android.extensions.*

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class MyView(context: Context) : View(context)

class MyActivity : Activity() {
    init { MyView(this).login }
}

// 1 public _\$_findCachedViewById
// 1 INVOKEVIRTUAL android/app/Activity\.findViewById
// 1 public _\$_clearFindViewByIdCache
// 1 GETSTATIC test/R\$id\.login
// 1 INVOKEVIRTUAL android/view/View\.findViewById
// 0 INVOKEVIRTUAL test/MyView\.findViewById
// 0 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 0 INVOKEVIRTUAL android/view/View\._\$_findCachedViewById
// 1 CHECKCAST android/widget/Button
