fun foo() {
    val cast1: /*T1@*/Int = 1/*LIT*/ as /*T0@*/Int/*T0@Int*/
    val cast2: /*T3@*/Int? = null/*NULL!!U*/ as /*T2@*/Int?/*T2@Int*/
    val cast3: /*T5@*/Float = (1/*LIT*/ as /*T4@*/Int/*T4@Int*/)/*T4@Int*/.toFloat()/*Float!!L*/
    val nya: /*T6@*/Int? = null/*NULL!!U*/
    val cast4: /*T8@*/Int? = nya/*T6@Int*/ as /*T7@*/Int?/*T7@Int*/
}

//LOWER <: T0 due to 'ASSIGNMENT'
//T0 <: T1 due to 'INITIALIZER'
//UPPER <: T2 due to 'ASSIGNMENT'
//T2 <: T3 due to 'INITIALIZER'
//LOWER <: T4 due to 'ASSIGNMENT'
//T4 := LOWER due to 'USE_AS_RECEIVER'
//LOWER <: T5 due to 'INITIALIZER'
//UPPER <: T6 due to 'INITIALIZER'
//T6 <: T7 due to 'ASSIGNMENT'
//T7 <: T8 due to 'INITIALIZER'
