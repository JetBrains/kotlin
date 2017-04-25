// EXPECTED_REACHABLE_NODES: 492
package foo

object A {
    object B {
        val lambda = { "OK" }
    }
}

fun box() = A.B.lambda()

