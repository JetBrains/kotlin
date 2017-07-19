package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.extensions.*

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
public class MyActivity : Activity() {
    init {login}
}

// 0 public _\$_findCachedViewById
// 0 public _\$_clearFindViewByIdCache
// 1 GETSTATIC test/R\$id\.login
// 0 INVOKEVIRTUAL test/MyActivity\._\$_findCachedViewById
// 1 INVOKEVIRTUAL android/app/Activity\.findViewById
// 1 CHECKCAST android/widget/Button