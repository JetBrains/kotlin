interface I {
    fun <caret>x() = 1
}

inline class Foo(val value: Int) : I {
    override fun x() = 2
}

// REF: (in Foo).x()