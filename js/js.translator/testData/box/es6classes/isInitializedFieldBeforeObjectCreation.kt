//WITH_RUNTIME
//IGNORE_BACKEND: JS

package test

import test.A.p

object A {
    lateinit var p: String
    var result: String

    init {
        result = if (::p.isInitialized) "FAIL" else "OK"
    }
}

fun box(): String {
    return A.result
}
