package test

import android.app.Fragment
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyFragment : Fragment() {
    init {login}
}

public class MyFragment2 : Fragment() {

    override fun onDestroy() {
        super.onDestroy()
    }

    public open fun onDestroy(n: Int) {}

}

// 2 public onDestroy\(\)V
// 1 INVOKEVIRTUAL test/MyFragment\._\$_clearFindViewByIdCache
// 1 INVOKEVIRTUAL test/MyFragment2\._\$_clearFindViewByIdCache