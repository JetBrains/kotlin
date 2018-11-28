// EXPECTED_REACHABLE_NODES: 1283
// This test was adapted from compiler/testData/codegen/box/callableReference/function/local/.
package foo

class A

fun box(): String {
    fun A.foo() = "OK"
    return (A::foo)(A())
}
