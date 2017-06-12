// EXPECTED_REACHABLE_NODES: 493
package foo

class Test() {
    val p = "OK"
}

fun box(): String {
    return Test().p
}