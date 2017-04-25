// EXPECTED_REACHABLE_NODES: 493
// See KT-12254
package foo

object A {
    var x: Int = throw RuntimeException("catch me")
}

fun box(): String {
    try {
        return "fail: ${A.x.toString()}"
    }
    catch (e: RuntimeException) {
        return if (e.message == "catch me") "OK" else "fail: ${e.message}"
    }
}