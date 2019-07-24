class Test {
    fun foo() {
        val x: /*T5@*/Array</*T4@*/Array</*T3@*/Int>> =
            Array</*T2@*/Array</*T1@*/Int>>(1/*LIT*/, {
                arrayOf</*T0@*/Int>(2/*LIT*/)/*Array<T0@Int>*/
            }/*Function0<Int, T6@Array<T7@Int>>*/
        )/*Array<T2@Array<T1@Int>>*/
    }
}

//LOWER <: T0 due to 'PARAMETER'
//T7 := T0 due to 'RETURN'
//T1 := T7 due to 'PARAMETER'
//T6 <: T2 due to 'PARAMETER'
//T3 := T1 due to 'INITIALIZER'
//T4 := T2 due to 'INITIALIZER'
