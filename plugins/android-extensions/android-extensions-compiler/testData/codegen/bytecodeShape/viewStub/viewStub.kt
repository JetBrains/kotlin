package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyActivity : Activity() {
    fun test() {
        stub
    }
}

// 1 GETSTATIC test/R\$id\.stub
// 0 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 2 INVOKEVIRTUAL android/app/Activity\.findViewById
// 1 CHECKCAST android/view/ViewStub
// 2 CHECKCAST android/view/View