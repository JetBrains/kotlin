// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1514
package foo


fun box(): String {
    val al = ArrayList<Int>(10)
    return if (al.size == 0) "OK" else "fail: ${al.size}"
}