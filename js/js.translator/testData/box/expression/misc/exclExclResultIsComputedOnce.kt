// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
package foo

var c = 0
val a: Int?
    get() {
        c++
        return 2
    }

fun box(): String {
    return if (c == 0 && (a!! + 3) == 5 && c == 1) return "OK" else "fail"
}