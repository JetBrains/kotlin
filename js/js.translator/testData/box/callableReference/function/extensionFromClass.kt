// EXPECTED_REACHABLE_NODES: 995
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

class A {
    fun result() = (A::bar)(this, "OK")
}

fun A.bar(x: String) = x

fun box() = A().result()
