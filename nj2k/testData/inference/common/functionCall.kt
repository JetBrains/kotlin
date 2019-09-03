fun test() {
    val x: /*T1@*/List</*T0@*/Int> = nya()/*T3@List<T2@Int>*/
}

fun nya(): /*T3@*/List</*T2@*/Int> {
    TODO()
}

//T2 <: T0 due to 'INITIALIZER'
//T3 <: T1 due to 'INITIALIZER'
