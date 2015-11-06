package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.*

public class MyActivity : Activity() {
    init { clearFindViewByIdCache() }
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL test/MyActivity\._\$_clearFindViewByIdCache
