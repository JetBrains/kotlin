/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/generics.1.kt
 */

package foo

import test.*

fun test1(s: Int): String {
    var result = "OK"
    result = mfun(s) { a ->
        result + doSmth(s) + doSmth(a)
    }

    return result
}

fun box(): String {
    val result = test1(11)
    if (result != "OK1111") return "fail1: ${result}"

    return "OK"
}