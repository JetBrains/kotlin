// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1108
package foo


fun box(): String {
    val a: Int? = 0

    val result = (a!! + 3)
    if (result != 3) return "fail: $result"
    return "OK"
}