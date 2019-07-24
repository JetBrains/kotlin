fun test() {
    val x: /*T2@*/List</*T1@*/Int> = List</*T0@*/Int>(10/*LIT*/, { 10/*LIT*/ }/*Function0<Int, T3@Int>*/)/*List<T0@Int>*/
}

//LOWER <: T3 due to 'RETURN'
//T3 <: T0 due to 'PARAMETER'
//T0 <: T1 due to 'INITIALIZER'
