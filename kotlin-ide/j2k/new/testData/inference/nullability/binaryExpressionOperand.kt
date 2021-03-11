fun test() {
    val x: /*T0@*/Int = 10/*LIT*/
    x/*T0@Int*/ > 0/*LIT*/
}

//LOWER <: T0 due to 'INITIALIZER'
//T0 := LOWER due to 'USE_AS_RECEIVER'
