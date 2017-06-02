package test

import android.view.View
import android.app.Activity
import android.content.Context
import kotlinx.android.synthetic.main.layout.view.*

class MyView(context: Context) : View(context)

class MyActivity : Activity() {
    init { MyView(this).login }
}

// 2 public _\$_findCachedViewById
// 1 INVOKEVIRTUAL test/MyActivity\.findViewById
// 2 public _\$_clearFindViewByIdCache
// 1 GETSTATIC test/R\$id\.login
// 0 INVOKEVIRTUAL android/view/View\.findViewById
// 1 INVOKEVIRTUAL test/MyView\.findViewById
// 0 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 0 INVOKEVIRTUAL android/view/View\._\$_findCachedViewById
// 1 CHECKCAST android/widget/Button
