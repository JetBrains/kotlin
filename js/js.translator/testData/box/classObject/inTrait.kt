// EXPECTED_REACHABLE_NODES: 994
package foo

interface A {
    companion object {
        val OK: String = "OK"
    }
}

fun box(): String {
    return A.OK
}