// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A {
    fun o() = 111
    fun k(k: Int) = k
}

fun A.bar() = this.(::o)() + this.(A::k)(222)

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"
    return "OK"
}
