// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A {
    fun foo() = "OK"
}

fun box(): String {
    val x = A::foo
    var r = x(A())
    return r
}
