package android.support.v4.app

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

open class FragmentManager {
    open fun findFragmentById(id: Int): Fragment = throw Exception("Function getFragmentById() is not overriden")
}

open class Fragment {
    open fun getFragmentManager(): FragmentManager = throw Exception("Function getFragmentManager() is not overriden")
}

open class FragmentActivity : Activity() {
    open fun getSupportFragmentManager(): FragmentManager = throw Exception("Function getSupportFragmentManager() is not overriden")
}

public class MyActivity : FragmentActivity() {
    init { fragm }
}

public class MyFragment : Fragment() {
    init { fragm }
}

// 1 INVOKEVIRTUAL android/support/v4/app/FragmentActivity\.getSupportFragmentManager
// 1 INVOKEVIRTUAL android/support/v4/app/Fragment\.getFragmentManager
// 2 GETSTATIC test/R\$id\.fragm
// 2 INVOKEVIRTUAL android/support/v4/app/FragmentManager\.findFragmentById
// 2 CHECKCAST android/support/v4/app/Fragment