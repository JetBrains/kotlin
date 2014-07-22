// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

abstract class A {
    abstract fun foo(): String
}

class B : A() {
    override fun foo() = "OK"
}

fun box(): String = B().(A::foo)()
