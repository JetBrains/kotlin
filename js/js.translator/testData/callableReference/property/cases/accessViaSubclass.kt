// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/property/.
package foo

abstract class Base {
    val result = "OK"
}

class Derived : Base()

fun box(): String {
    return (Base::result).get(Derived())
}
