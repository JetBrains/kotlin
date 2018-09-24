// EXPECTED_REACHABLE_NODES: 1282
package foo

class Test() {
    val p = "OK"
}

fun box(): String {
    return Test().p
}