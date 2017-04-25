// EXPECTED_REACHABLE_NODES: 494
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    fun o() = 111
    fun k(k: Int) = k
}

fun A.bar() = (A::o)(this) + (A::k)(this, 222)

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"
    return "OK"
}
