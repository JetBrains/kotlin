package test

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

fun test(obj: Any?) {
    obj as Activity
    obj.login
}