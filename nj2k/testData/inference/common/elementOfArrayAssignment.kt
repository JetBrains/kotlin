fun test() {
    val x: /*T4@*/Array</*T3@*/Array</*T2@*/Int>> = arrayOf</*T1@*/Array</*T0@*/Int>>()/*Array<T1@Array<T0@Int>>*/
    val y: /*T6@*/Array</*T5@*/Int> = x/*T4@Array<T3@Array<T2@Int>>*/.get(0/*LIT*/)/*T3@Array<T2@Int>*/
    val z: /*T7@*/Int = y/*T6@Array<T5@Int>*/.get(0/*LIT*/)/*T5@Int*/
}

//T2 := T0 due to 'INITIALIZER'
//T3 := T1 due to 'INITIALIZER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T3 := T3 due to 'RECEIVER_PARAMETER'
//T5 := T2 due to 'INITIALIZER'
//T3 <: T6 due to 'INITIALIZER'
//T5 := T5 due to 'RECEIVER_PARAMETER'
//T5 <: T7 due to 'INITIALIZER'
