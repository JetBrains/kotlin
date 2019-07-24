open class A<T> {
    open fun foo(x: /*T1@*/List</*T0@*/T>, y: /*T2@*/Boolean, z: /*T3@*/T) {
        TODO()
    }
}

class B : A</*T8@*/Int>() {
    override fun foo(x: /*T5@*/List</*T4@*/Int>, y: /*T6@*/Boolean, z: /*T7@*/Int) {
        TODO()
    }
}

//T4 := T8 due to 'SUPER_DECLARATION'
//T1 := T5 due to 'SUPER_DECLARATION'
//T2 := T6 due to 'SUPER_DECLARATION'
//T7 := T8 due to 'SUPER_DECLARATION'
