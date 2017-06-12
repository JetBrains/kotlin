// EXPECTED_REACHABLE_NODES: 493
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    fun bar(k: Int) = k

    fun result() = (A::bar)(this, 111)
}

fun box(): String {
    val result = A().result()
    if (result != 111) return "Fail $result"
    return "OK"
}
