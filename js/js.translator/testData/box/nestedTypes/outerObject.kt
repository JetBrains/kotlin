// EXPECTED_REACHABLE_NODES: 495
package foo

val q = "baz"

object A {
    val x = "foo"

    class B {
        val y = x + "_bar"
        val z = q + "_bar"
    }
}

fun box(): String {
    var result = A.B().y
    if (result != "foo_bar") {
        return "failed1_" + result
    }
    result = A.B().z
    if (result != "baz_bar") {
        return "failed2_" + result
    }
    return "OK"
}