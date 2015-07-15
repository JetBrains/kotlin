// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A {
    fun o() = 111
    fun k(k: Int) = k
}

fun A.bar() = (::o)(this) + (A::k)(this, 222)

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"
    return "OK"
}
