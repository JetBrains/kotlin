// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A(val result: String)

fun box() = (::A)("OK").result
