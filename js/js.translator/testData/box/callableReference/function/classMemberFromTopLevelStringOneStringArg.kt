// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1112
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    fun foo(result: String):String = result
}

fun box(): String {
    val x = A::foo
    var r = x(A(), "OK")

    return r
}
