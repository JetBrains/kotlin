// EXPECTED_REACHABLE_NODES: 1114
package foo

class Test() {
    val p = "OK"
}

fun box(): String {
    return Test().p
}