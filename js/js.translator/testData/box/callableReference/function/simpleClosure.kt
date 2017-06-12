// EXPECTED_REACHABLE_NODES: 489
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

fun box(): String {
    val result = "OK"

    fun foo() = result

    return (::foo)()
}
