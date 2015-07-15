// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A

fun A.foo() = (A::bar)(this, "OK")

fun A.bar(x: String) = x

fun box() = A().foo()
