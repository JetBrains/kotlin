open class A<T> {
    open fun foo(): /*T0@*/T {
        TODO()
    }
}

class B<T> : A</*T2@*/Int>() {
    override fun foo(): /*T1@*/Int {
        TODO()
    }
}

//T1 := T2 due to 'SUPER_DECLARATION'
