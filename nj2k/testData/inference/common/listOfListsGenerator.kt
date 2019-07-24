fun test() {
    val x: /*T5@*/List</*T4@*/List</*T3@*/Int>> = List</*T2@*/List</*T1@*/Int>>(13/*LIT*/, {
             List</*T0@*/Int>(7/*LIT*/, {
                 666/*LIT*/
             }/*Function0<Int, T6@Int>*/)/*List<T0@Int>*/
         }/*Function0<Int, T7@List<T8@Int>>*/
    )/*List<T2@List<T1@Int>>*/
}

//LOWER <: T6 due to 'RETURN'
//T6 <: T0 due to 'PARAMETER'
//T0 <: T8 due to 'RETURN'
//T8 <: T1 due to 'PARAMETER'
//T7 <: T2 due to 'PARAMETER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
