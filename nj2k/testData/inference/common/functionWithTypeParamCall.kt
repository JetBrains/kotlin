fun test() {
    val x: /*T3@*/Map</*T1@*/String, /*T2@*/Int> = nya</*T0@*/String>()/*T6@Map<T0@String, T5@Int>*/
}

fun <T> nya(): /*T6@*/Map</*T4@*/T, /*T5@*/Int> {
    TODO()
}

//T1 := T0 due to 'INITIALIZER'
//T5 <: T2 due to 'INITIALIZER'
//T6 <: T3 due to 'INITIALIZER'
