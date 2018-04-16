// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1111
package foo

class A() {
    val p = { "OK" }
}


fun box(): String {
    return A().p()
}