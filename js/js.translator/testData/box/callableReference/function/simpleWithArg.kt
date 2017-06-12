// EXPECTED_REACHABLE_NODES: 489
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

fun box(): String {
    fun foo(s: String) = s
    return (::foo)("OK")
}
