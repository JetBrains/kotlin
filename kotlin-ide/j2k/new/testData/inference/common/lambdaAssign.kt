fun test() {
    val x: /*T5@*/Function2</*T2@*/Int, /*T3@*/String, /*T4@*/Boolean> = { a: /*T0@*/Int, b: /*T1@*/String -> true/*LIT*/ }/*Function2<T0@Int, T1@String, T6@Boolean>*/
}

//LOWER <: T6 due to 'RETURN'
//T2 <: T0 due to 'INITIALIZER'
//T3 <: T1 due to 'INITIALIZER'
//T6 <: T4 due to 'INITIALIZER'
