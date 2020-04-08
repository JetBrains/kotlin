package testing

interface I {
    val <caret>p: Int
        get() = 0
}

class A(i: I) : I by i

class B(i: I) : I by i {
    override val p = 5
}

class C(i: I) : I by i


// REF: (in testing.B).p


