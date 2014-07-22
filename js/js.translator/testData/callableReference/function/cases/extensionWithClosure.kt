// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/local/.
package foo

class A

fun box(): String {
    var result = "Fail"

    fun A.ext() { result = "OK" }

    val f = A::ext
    A().f()
    return result
}
