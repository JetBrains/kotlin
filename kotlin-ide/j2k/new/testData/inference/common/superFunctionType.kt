open class A {
    open fun foo(): /*T0@*/String {
        TODO()
    }
}

class B : A() {
    override fun foo(): /*T1@*/String {
        TODO()
    }
}

//T0 := T1 due to 'SUPER_DECLARATION'
