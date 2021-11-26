// EXPECTED_REACHABLE_NODES: 1342
// WITH_STDLIB

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
