fun test() {
    val x: /*T0@*/Int = 10/*LIT*/
    val y: /*T1@*/Int = x/*T0@Int*/
    val z: /*T2@*/Int = y/*T1@Int*/
}

//LOWER <: T0 due to 'INITIALIZER'
//T0 <: T1 due to 'INITIALIZER'
//T1 <: T2 due to 'INITIALIZER'
