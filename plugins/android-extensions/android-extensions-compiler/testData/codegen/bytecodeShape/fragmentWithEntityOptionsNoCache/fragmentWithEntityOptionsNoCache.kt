package test

import android.app.Fragment
import java.io.File
import kotlinx.android.synthetic.main.layout.*
import kotlinx.android.extensions.*

@ContainerOptions(cache = CacheImplementation.NO_CACHE)
public class MyFragment : Fragment() {
    init {login}
}

// 0 public _\$_findCachedViewById
// 0 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL android/app/Fragment\.getView
// 1 GETSTATIC test/R\$id\.login
// 0 INVOKEVIRTUAL test/MyFragment\._\$_findCachedViewById
// 1 INVOKEVIRTUAL android/app/Fragment\.getView
// 1 INVOKEVIRTUAL android/view/View\.findViewById
// 1 CHECKCAST android/widget/Button