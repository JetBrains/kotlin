fun test() {
    val x: /*T2@*/List</*T1@*/String> = listOf</*T0@*/String>()/*List<T0@String>*/
    for (y: /*T3@*/String in x/*T2@List<T1@String>*/) {
        val z: /*T4@*/String = x/*T2@List<T1@String>*/
    }
}

//T0 <: T1 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
//T3 <: T1 due to 'ASSIGNMENT'
