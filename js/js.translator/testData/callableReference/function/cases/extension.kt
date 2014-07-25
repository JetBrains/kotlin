// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

class A

fun box(): String {
    fun A.foo() = "OK"
    return A().(A::foo)()
}
