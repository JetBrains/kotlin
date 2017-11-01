// EXPECTED_REACHABLE_NODES: 1251
package foo

class Test() {
    val p = "OK"
}

fun box(): String {
    return Test().p
}