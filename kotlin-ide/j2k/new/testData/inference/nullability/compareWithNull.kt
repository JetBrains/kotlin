fun a(): /*T0@*/Int? {
    return 42/*LIT*/
}

val b: /*T1@*/Int? = 2/*LIT*/

fun c(p: /*T2@*/Int?) {
    if (p/*T2@Int*/ == null/*LIT*/);
}

fun check() {
    if (a()/*T0@Int*/ == null/*LIT*/ || b/*T1@Int*/ == null/*LIT*//*LIT*/);
}

//LOWER <: T0 due to 'RETURN'
//LOWER <: T1 due to 'INITIALIZER'
//T2 := UPPER due to 'COMPARE_WITH_NULL'
//T0 := UPPER due to 'COMPARE_WITH_NULL'
//T1 := UPPER due to 'COMPARE_WITH_NULL'
