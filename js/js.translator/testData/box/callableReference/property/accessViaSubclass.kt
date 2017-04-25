// EXPECTED_REACHABLE_NODES: 500
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

abstract class Base {
    val result = "OK"
}

class Derived : Base()

fun box(): String {
    return (Base::result).get(Derived())
}
