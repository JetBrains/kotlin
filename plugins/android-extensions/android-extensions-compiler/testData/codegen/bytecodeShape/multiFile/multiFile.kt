package test

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class MyActivity: Activity() {
    val button = this.login
    val button1 = this.loginButton
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 GETSTATIC test/R\$id\.login : I
// 1 GETSTATIC test/R\$id\.loginButton : I
// 2 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button