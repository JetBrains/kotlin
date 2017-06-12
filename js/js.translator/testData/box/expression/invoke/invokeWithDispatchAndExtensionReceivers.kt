// EXPECTED_REACHABLE_NODES: 490
package foo

class A

fun box(): String {
    val a = A()
    val b = fun A.(i: Int) = i
    val result = a.(b)(1)
    if (result != 1) return "fail: $result"
    return "OK"
}
