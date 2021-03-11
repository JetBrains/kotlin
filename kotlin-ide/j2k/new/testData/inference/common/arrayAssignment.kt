fun test() {
    val x: /*T2@*/Array</*T1@*/Int> = arrayOf</*T0@*/Int>()/*Array<T0@Int>*/
    val y: /*T4@*/Array</*T3@*/Int> = x/*T2@Array<T1@Int>*/
}

//T1 := T0 due to 'INITIALIZER'
//T3 := T1 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
