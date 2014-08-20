// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A

fun A.foo() = this.(A::bar)("OK")

fun A.bar(x: String) = x

fun box() = A().foo()
