// EXPECTED_REACHABLE_NODES: 1112
package foo

class A {
    operator fun invoke(i: Int) = i
}

fun box(): String {
    val result = A()(1)
    if (result != 1) return "fail: $result"
    return "OK"
}
