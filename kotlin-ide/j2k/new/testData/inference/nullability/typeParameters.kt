fun foo() {
    val a: /*T5@*/List</*T4@*/List</*T3@*/Int>> = listOf</*T2@*/List</*T1@*/Int>>(
        listOf</*T0@*/Int>(
            1/*LIT*/
        )/*List<T0@Int>!!L*/
    )/*List<T2@List<T1@Int>>!!L*/
    val b: /*T7@*/List</*T6@*/Int> = a/*T5@List<T4@List<T3@Int>>*/.get(0/*LIT*/)/*T4@List<T3@Int>*/
}

//LOWER <: T0 due to 'PARAMETER'
//T0 <: T1 due to 'PARAMETER'
//LOWER <: T2 due to 'PARAMETER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
//LOWER <: T5 due to 'INITIALIZER'
//T3 <: T3 due to 'RECEIVER_PARAMETER'
//T4 <: T4 due to 'RECEIVER_PARAMETER'
//T5 := LOWER due to 'USE_AS_RECEIVER'
//T3 <: T6 due to 'INITIALIZER'
//T4 <: T7 due to 'INITIALIZER'
