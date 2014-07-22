// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

fun box(): String {
    fun foo(s: String) = s
    return (::foo)("OK")
}
