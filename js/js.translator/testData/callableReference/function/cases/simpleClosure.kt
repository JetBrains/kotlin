// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

fun box(): String {
    val result = "OK"

    fun foo() = result

    return (::foo)()
}
