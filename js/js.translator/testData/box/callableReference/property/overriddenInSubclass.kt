// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1123
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

open class Base {
    open val foo = "Base"
}

class Derived : Base() {
    override val foo = "OK"
}

fun box() = (Base::foo).get(Derived())
