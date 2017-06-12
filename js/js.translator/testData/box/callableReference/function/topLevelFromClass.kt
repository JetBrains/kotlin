// EXPECTED_REACHABLE_NODES: 494
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

fun <T> run(arg1: T, arg2: T, funRef:(T,T) -> T): T {
    return funRef(arg1, arg2)
}

fun tmp(o: Int, k: Int) = o + k

class A {
    fun bar() = (::tmp)(111, 222)
}

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"

    var r = run(111, 222, ::tmp)
    if (result != 333) return "Fail $result"

    return "OK"
}
