package test

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*

class MyActivity: Activity() {
    val button = this.MyButton
    val button2 = MyButton
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 2 GETSTATIC test/R\$id\.MyButton
// 2 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button
