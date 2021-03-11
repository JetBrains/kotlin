val x: /*T0@*/Int? = null/*NULL!!U*/
fun foo(p: /*T1@*/Int? = null/*NULL!!U*/) {}

//UPPER <: T0 due to 'INITIALIZER'
//UPPER <: T1 due to 'INITIALIZER'
