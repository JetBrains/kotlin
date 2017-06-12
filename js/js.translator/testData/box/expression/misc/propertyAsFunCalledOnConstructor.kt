// EXPECTED_REACHABLE_NODES: 490
package foo

class A() {
    val p = { "OK" }
}


fun box(): String {
    return A().p()
}