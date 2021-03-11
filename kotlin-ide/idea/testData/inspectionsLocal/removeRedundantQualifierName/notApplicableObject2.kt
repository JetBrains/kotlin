// PROBLEM: none
// WITH_RUNTIME
class A {
    companion object {
        val INST = A()
    }
}

class B {
    companion object {
        val INST = B()
    }

    fun foo() {
        <caret>A.INST.toString()
    }
}