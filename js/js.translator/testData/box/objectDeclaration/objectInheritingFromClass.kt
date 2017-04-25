// EXPECTED_REACHABLE_NODES: 498
package foo

abstract class A(val s: String) {
}

object B : A("test") {
}

fun box() = if (B.s == "test") "OK" else "fail: ${B.s}"
