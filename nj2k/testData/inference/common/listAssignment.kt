fun test() {
    val x: /*T2@*/List</*T1@*/Int> = listOf</*T0@*/Int>()/*List<T0@Int>*/
    val y: /*T4@*/List</*T3@*/Int> = x/*T2@List<T1@Int>*/
}

//T0 <: T1 due to 'INITIALIZER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
