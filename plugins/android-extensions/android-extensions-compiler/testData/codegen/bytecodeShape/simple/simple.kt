package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyActivity : Activity() {
    init {login}
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 GETSTATIC test/R\$id\.login
// 1 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 1 CHECKCAST android/widget/Button
// 0 android/util/SparseArray
// 12 java/util/HashMap