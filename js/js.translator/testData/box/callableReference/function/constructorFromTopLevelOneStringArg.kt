// EXPECTED_REACHABLE_NODES: 1282
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A(val result: String)

fun box() = (::A)("OK").result
