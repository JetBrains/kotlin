// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    var result = "OK"
}

fun box() = (::A)().result
