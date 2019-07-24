class Test {
    fun foo1(r: /*T2@*/Function1</*T0@*/Int, /*T1@*/String?>) {}
    fun foo() {
        foo1({ x: /*T3@*/Int -> ""/*LIT*/ }/*Function1<T3@Int, T5@String>!!L*/)
        foo1({ i: /*T4@*/Int ->
                 if (i/*T4@Int*/ > 1/*LIT*//*LIT*/) {
                     return@foo1 null/*NULL!!U*/
                 }
                 ""/*LIT*/
             }/*Function1<T4@Int, T6@String>!!L*/)
    }
}

//LOWER <: T5 due to 'RETURN'
//T0 <: T3 due to 'PARAMETER'
//T5 <: T1 due to 'PARAMETER'
//LOWER <: T2 due to 'PARAMETER'
//T4 := LOWER due to 'USE_AS_RECEIVER'
//UPPER <: T6 due to 'RETURN'
//LOWER <: T6 due to 'RETURN'
//T0 <: T4 due to 'PARAMETER'
//T6 <: T1 due to 'PARAMETER'
//LOWER <: T2 due to 'PARAMETER'
