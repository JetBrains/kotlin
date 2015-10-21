package com.myapp

import android.view.View
import android.app.Activity
import kotlinx.android.synthetic.main.layout.view.*

public class MyActivity : Activity() {
    init { View(this).login }
}

// 1 public _\$_findCachedViewById
// 1 INVOKEVIRTUAL com/myapp/MyActivity\.findViewById
// 1 public _\$_clearFindViewByIdCache
// 1 GETSTATIC com/myapp/R\$id\.login
// 1 INVOKEVIRTUAL android/view/View\.findViewById
// 0 INVOKEVIRTUAL com/myapp/MyActivity\._\$_findCachedViewById
// 1 CHECKCAST android/widget/Button