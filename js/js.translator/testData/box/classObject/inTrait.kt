// EXPECTED_REACHABLE_NODES: 1284
package foo

interface A {
    companion object {
        val OK: String = "OK"
    }
}

fun box(): String {
    return A.OK
}