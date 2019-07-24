open class A<T> {
    open fun foo(): /*T3@*/Map</*T0@*/T, /*T2@*/List</*T1@*/T>> {
        TODO()
    }

    open fun bar(): /*T4@*/T {
        TODO()
    }
}

class B : A</*T9@*/Int>() {
    override fun foo(): /*T8@*/Map</*T5@*/Int, /*T7@*/List</*T6@*/Int>> {
        TODO()
    }
}

//T5 := T9 due to 'SUPER_DECLARATION'
//T6 := T9 due to 'SUPER_DECLARATION'
//T2 := T7 due to 'SUPER_DECLARATION'
//T3 := T8 due to 'SUPER_DECLARATION'
