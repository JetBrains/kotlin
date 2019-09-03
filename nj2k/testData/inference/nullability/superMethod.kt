open class A () {
    open fun foo(x: /*T0@*/Int?): /*T1@*/Int? {
        if (x/*T0@Int*/ == null/*LIT*/);
        return null/*NULL!!U*/
    }
}

class B : A() {
    override fun foo(x: /*T2@*/Int?): /*T3@*/Int? {
        return 1/*LIT*/
    }
}

//T0 := UPPER due to 'COMPARE_WITH_NULL'
//UPPER <: T1 due to 'RETURN'
//LOWER <: T3 due to 'RETURN'
//T1 := T3 due to 'SUPER_DECLARATION'
//T0 := T2 due to 'SUPER_DECLARATION'
