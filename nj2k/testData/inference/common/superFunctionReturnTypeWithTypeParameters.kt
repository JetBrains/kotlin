interface A<T, S> {
    fun foo(): /*T3@*/List</*T2@*/Map</*T0@*/T, /*T1@*/S>>
}


open class B<X> : A</*T8@*/X, /*T9@*/Int> {
    override fun foo(): /*T7@*/List</*T6@*/Map</*T4@*/X, /*T5@*/Int>> {
        TODO()
    }
}

//T4 := T8 due to 'SUPER_DECLARATION'
//T5 := T9 due to 'SUPER_DECLARATION'
//T2 := T6 due to 'SUPER_DECLARATION'
//T3 := T7 due to 'SUPER_DECLARATION'
