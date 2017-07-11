// EXPECTED_REACHABLE_NODES: 996
package foo

class Test() {
    val p = "OK"
}

fun box(): String {
    return Test().p
}