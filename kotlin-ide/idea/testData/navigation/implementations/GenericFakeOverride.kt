package testing

open class C<T> {
    open fun <caret>f(): T {

    }
}

class A : C<Int>()

class B : C<String>() {
    override fun f() = "A"
}

// REF: (in testing.B).f()
