fun <T, E, F, S> foo(x: /*T0@*/T, y: /*T2@*/List</*T1@*/E>, z: /*T4@*/List</*T3@*/F>, a: /*T5@*/S) {}

fun bar() {
    val lst: /*T8@*/List</*T7@*/Int?> = listOf</*T6@*/Int?>(null/*NULL!!U*/)/*List<T6@Int>!!L*/
    val lst2: /*T11@*/List</*T10@*/Int> = listOf</*T9@*/Int>(1/*LIT*/)/*List<T9@Int>!!L*/
    foo</*T12@*/Int?, /*T13@*/Int?, /*T14@*/Int, /*T15@*/String>(null/*NULL!!U*/, lst/*T8@List<T7@Int>*/, lst2/*T11@List<T10@Int>*/, "nya"/*LIT*/)
}

//UPPER <: T6 due to 'PARAMETER'
//T6 <: T7 due to 'INITIALIZER'
//LOWER <: T8 due to 'INITIALIZER'
//LOWER <: T9 due to 'PARAMETER'
//T9 <: T10 due to 'INITIALIZER'
//LOWER <: T11 due to 'INITIALIZER'
//UPPER <: T12 due to 'PARAMETER'
//T7 <: T13 due to 'PARAMETER'
//T8 <: T2 due to 'PARAMETER'
//T10 <: T14 due to 'PARAMETER'
//T11 <: T4 due to 'PARAMETER'
//LOWER <: T15 due to 'PARAMETER'
