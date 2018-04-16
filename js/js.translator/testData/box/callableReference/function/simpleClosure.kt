// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

fun box(): String {
    val result = "OK"

    fun foo() = result

    return (::foo)()
}
