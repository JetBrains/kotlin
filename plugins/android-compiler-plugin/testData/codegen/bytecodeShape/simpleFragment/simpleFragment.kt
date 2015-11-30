package test

import android.app.Fragment
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyFragment : Fragment() {
    init {login}
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL test/MyFragment\.getView
// 1 GETSTATIC test/R\$id\.login
// 1 INVOKEVIRTUAL test/MyFragment\._\$_findCachedViewById
// 1 CHECKCAST android/widget/Button