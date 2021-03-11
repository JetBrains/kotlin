fun f(): /*T1@*/Int {
    val x: /*T0@*/Int = 42/*LIT*/
    return x/*T0@Int*/
}

//LOWER <: T0 due to 'INITIALIZER'
//T0 <: T1 due to 'RETURN'
