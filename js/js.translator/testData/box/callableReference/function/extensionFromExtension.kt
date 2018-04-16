// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1112
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A

fun A.foo() = (A::bar)(this, "OK")

fun A.bar(x: String) = x

fun box() = A().foo()
