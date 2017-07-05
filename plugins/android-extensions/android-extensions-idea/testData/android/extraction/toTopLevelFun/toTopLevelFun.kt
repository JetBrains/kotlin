// SUGGESTED_NAMES: view, getButton
// SUGGESTED_RETURN_TYPES: android.view.View?, android.view.View
// PARAM_DESCRIPTOR: public final class MyActivity : android.app.Activity defined in test in file toTopLevelFun.kt
// PARAM_TYPES: test.MyActivity, android.app.Activity

package test

import android.app.Activity
import kotlinx.android.synthetic.main.layout.*

// SIBLING:

class MyActivity: Activity() {
    fun test() {
        val button = <selection>MyButton</selection>
    }
}