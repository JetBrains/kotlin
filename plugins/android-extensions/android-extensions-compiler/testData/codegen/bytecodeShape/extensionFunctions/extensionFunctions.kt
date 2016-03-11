package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyActivity : Activity() {

}

fun MyActivity.b() {
    val x = login
    val y = this.login
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 2 GETSTATIC test/R\$id\.login
// 2 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button
