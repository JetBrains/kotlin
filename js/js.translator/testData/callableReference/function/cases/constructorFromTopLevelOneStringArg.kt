// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

class A(val result: String)

fun box() = (::A)("OK").result
