package testing

interface I {
    fun <caret>f() {

    }
}

class A : I

class B : I {
    override fun f() {
    }
}

class C : I

interface II: I
interface III: I {
    override fun f() {
    }
}

// REF: (in testing.B).f()
// REF: (in testing.III).f()

