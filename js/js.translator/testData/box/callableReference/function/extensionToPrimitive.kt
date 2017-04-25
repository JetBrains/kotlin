// EXPECTED_REACHABLE_NODES: 489
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

fun box(): String {
    fun Int.is42With(that: Int) = this + 2 * that == 42
    return if ((Int::is42With)(16, 13)) "OK" else "Fail"
}
