// EXPECTED_REACHABLE_NODES: 491
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

class A

fun box(): String {
    var result = "Fail"

    fun A.ext() { result = "OK" }

    val f = A::ext
    f(A())
    return result
}
