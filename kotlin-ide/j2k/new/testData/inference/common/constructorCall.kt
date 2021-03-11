fun test() {
    val x: /*T2@*/X</*T1@*/Int> = X</*T0@*/Int>(10/*LIT*/)/*X<T0@Int>*/
    val x: /*T5@*/X</*T4@*/Int> = X</*T3@*/Int>(10/*LIT*/, 20/*LIT*/)/*X<T3@Int>*/
}

class X<T>(x: /*T6@*/T) {
    constructor(x: /*T7@*/T, y: /*T8@*/Int): this(x/*T7@T*/)
}

//LOWER <: T0 due to 'PARAMETER'
//T1 := T0 due to 'INITIALIZER'
//LOWER <: T3 due to 'PARAMETER'
//LOWER <: T8 due to 'PARAMETER'
//T4 := T3 due to 'INITIALIZER'
//T7 <: T6 due to 'PARAMETER'
