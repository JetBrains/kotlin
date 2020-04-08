fun foo() {
    val a: /*T5@*/List</*T4@*/List</*T3@*/Int>> = listOf</*T2@*/List</*T1@*/Int>>(
        listOf</*T0@*/Int>(
            1/*LIT*/
        )/*List<T0@Int>*/
    )/*List<T2@List<T1@Int>>*/
}

//LOWER <: T0 due to 'PARAMETER'
//T0 <: T1 due to 'PARAMETER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T4 due to 'INITIALIZER'
