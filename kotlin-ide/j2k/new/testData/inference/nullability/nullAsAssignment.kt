fun test() {
    var x: /*T0@*/Int? = 1/*LIT*/
    x/*T0@Int*/ = null/*NULL!!U*/

    var y: /*T1@*/Int? = 5/*LIT*/
    y/*T1@Int*/ = nullableFun()/*T2@Int*/
}

fun nullableFun(): /*T2@*/Int? {
    return null/*NULL!!U*/
}

//LOWER <: T0 due to 'INITIALIZER'
//UPPER <: T0 due to 'ASSIGNMENT'
//T0 := UPPER due to 'COMPARE_WITH_NULL'
//LOWER <: T1 due to 'INITIALIZER'
//T2 <: T1 due to 'ASSIGNMENT'
//UPPER <: T2 due to 'RETURN'
