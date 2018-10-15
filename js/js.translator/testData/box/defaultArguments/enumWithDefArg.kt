// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1291
package foo

enum class A(val a: Int = 1) {
    FIRST(),
    SECOND(2)
}

fun box(): String {
    if (A.FIRST.a == 1 && A.SECOND.a == 2) {
        return "OK"
    }
    return "fail"
}
