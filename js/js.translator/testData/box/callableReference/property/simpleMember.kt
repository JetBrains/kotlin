// EXPECTED_REACHABLE_NODES: 493
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

class A(val x: Int)

fun box(): String {
    val p = A::x
    if (p.get(A(42)) != 42) return "Fail 1"
    if (p.get(A(-1)) != -1) return "Fail 2"
    if (p.name != "x") return "Fail 3"
    return "OK"
}
