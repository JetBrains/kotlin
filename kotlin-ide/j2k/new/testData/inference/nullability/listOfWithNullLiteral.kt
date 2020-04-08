fun test() {
    val x: /*T2@*/List</*T1@*/Int?> = listOf</*T0@*/Int?>(1/*LIT*/, null/*NULL!!U*/, 3/*LIT*/)/*List<T0@Int>!!L*/
}

//LOWER <: T0 due to 'PARAMETER'
//UPPER <: T0 due to 'PARAMETER'
//LOWER <: T0 due to 'PARAMETER'
//T0 <: T1 due to 'INITIALIZER'
//LOWER <: T2 due to 'INITIALIZER'
