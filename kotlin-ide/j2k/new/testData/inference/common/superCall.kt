open class A<T> {
    open fun foo(x: /*T0@*/T, y: /*T2@*/List</*T1@*/T>) {
    }
}

class B : A</*T6@*/Int>() {
    override fun foo(x: /*T3@*/Int, y: /*T5@*/List</*T4@*/Int>) {
        super/*LIT*/.foo(x/*T3@Int*/, y/*T5@List<T4@Int>*/)
    }
}

//T3 := T6 due to 'SUPER_DECLARATION'
//T4 := T6 due to 'SUPER_DECLARATION'
//T2 := T5 due to 'SUPER_DECLARATION'
