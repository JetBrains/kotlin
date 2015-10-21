package com.myapp

import android.app.Activity
import android.app.Fragment
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.synthetic.main.layout1.*

class MyActivity: Activity() {
    val button = this.login
}

class MyFragment: Fragment() {
    val button1 = this.loginButton
}

// 2 public _\$_findCachedViewById
// 2 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL com/myapp/MyFragment\.getView
// 1 GETSTATIC com/myapp/R\$id\.login : I
// 1 GETSTATIC com/myapp/R\$id\.loginButton : I
// 1 INVOKEVIRTUAL com/myapp/MyActivity\._\$_findCachedViewById
// 1 INVOKEVIRTUAL com/myapp/MyFragment\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button