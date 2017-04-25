// EXPECTED_REACHABLE_NODES: 499
// This test was adapted from compiler/testData/codegen/box/callableReference/function/.
package foo

abstract class A {
    abstract fun foo(): String
}

class B : A() {
    override fun foo() = "OK"
}

fun box(): String = (A::foo)(B())
