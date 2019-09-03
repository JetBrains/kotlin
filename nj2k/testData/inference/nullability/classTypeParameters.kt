open class A<T> {
    open fun foo(): /*T3@*/Map</*T0@*/T, /*T2@*/List</*T1@*/T>>? {
        TODO()
    }

    open fun bar(): /*T4@*/T? {
        return null/*NULL!!U*/
    }
}

class B : A</*T10@*/Int>() {
    override fun foo(): /*T8@*/Map</*T5@*/Int, /*T7@*/List</*T6@*/Int>>? {
        return null/*NULL!!U*/
    }
    override fun bar(): /*T9@*/Int {
        return 42/*LIT*/
    }
}

//UPPER <: T4 due to 'RETURN'
//UPPER <: T8 due to 'RETURN'
//T5 := T10 due to 'SUPER_DECLARATION'
//T6 := T10 due to 'SUPER_DECLARATION'
//T2 := T7 due to 'SUPER_DECLARATION'
//T3 := T8 due to 'SUPER_DECLARATION'
//LOWER <: T9 due to 'RETURN'
//T9 := T10 due to 'SUPER_DECLARATION'
