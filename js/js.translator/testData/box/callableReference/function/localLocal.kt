// EXPECTED_REACHABLE_NODES: 490
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

fun box(): String {
    fun foo(): String {
        fun bar() = "OK"
        val ref = ::bar
        return ref()
    }

    val ref = ::foo
    return ref()
}
