// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

fun foo(until: Int): String {
    fun bar(x: Int): String =
            if (x == until) "OK" else bar(x + 1)
    return (::bar)(0)
}

fun box() = foo(10)
