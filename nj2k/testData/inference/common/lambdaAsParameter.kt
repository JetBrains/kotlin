fun test() {
    nya</*T2@*/Boolean>({ a: /*T0@*/Int, b: /*T1@*/Boolean -> ""/*LIT*/ }/*Function2<T0@Int, T1@Boolean, T7@String>*/)
}

fun <T> nya(x: /*T6@*/Function2</*T3@*/T, /*T4@*/Boolean, /*T5@*/String>) {
}

//LOWER <: T7 due to 'RETURN'
//T2 <: T0 due to 'PARAMETER'
//T4 <: T1 due to 'PARAMETER'
//T7 <: T5 due to 'PARAMETER'
