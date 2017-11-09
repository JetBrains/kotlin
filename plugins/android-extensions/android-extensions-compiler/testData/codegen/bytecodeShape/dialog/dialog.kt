package test

import android.app.Dialog
import kotlinx.android.synthetic.main.layout.*

fun test(dialog: Dialog) {
    dialog.login
}

// 1 GETSTATIC test/R\$id\.login
// 1 INVOKEVIRTUAL android/app/Dialog\.findViewById
// 1 CHECKCAST android/widget/Button
// 0 _\$_findCachedViewById