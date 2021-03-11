fun test() {
    val x: /*T4@*/List</*T3@*/List</*T2@*/Int>> = listOf</*T1@*/List</*T0@*/Int>>()/*List<T1@List<T0@Int>>*/
    val y: /*T6@*/List</*T5@*/Int> = x/*T4@List<T3@List<T2@Int>>*/.get(0/*LIT*/)/*T3@List<T2@Int>*/
    val z: /*T7@*/Int = y/*T6@List<T5@Int>*/.get(0/*LIT*/)/*T5@Int*/
}

//T0 <: T2 due to 'INITIALIZER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T2 due to 'RECEIVER_PARAMETER'
//T3 <: T3 due to 'RECEIVER_PARAMETER'
//T2 <: T5 due to 'INITIALIZER'
//T3 <: T6 due to 'INITIALIZER'
//T5 <: T5 due to 'RECEIVER_PARAMETER'
//T5 <: T7 due to 'INITIALIZER'
