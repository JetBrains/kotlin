// SIBLING:
class A {
    fun foo() {
        val a = B()
        <selection>a.unresolved()</selection>
    }

    private fun B.unresolved() = 1
}

class B

private fun A.unresolved() = 1