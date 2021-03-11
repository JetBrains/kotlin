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

class A1(i: I) : I by i

class B1(i: I) : I by i {
    override fun f() {
    }
}

class C1(i: I) : I by i

// REF: (in testing.B).f()
// REF: (in testing.B1).f()
// REF: (in testing.III).f()

