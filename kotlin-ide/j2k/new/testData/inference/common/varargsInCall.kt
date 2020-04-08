fun test() {
    val x: /*T0@*/String = ""/*LIT*/
    val y: /*T1@*/String = ""/*LIT*/
    val z: /*T2@*/String = ""/*LIT*/
    val e: /*T5@*/List</*T4@*/String> = listOf</*T3@*/String>(x/*T0@String*/, y/*T1@String*/, z/*T2@String*/)/*List<T3@String>*/
}

//LOWER <: T0 due to 'INITIALIZER'
//LOWER <: T1 due to 'INITIALIZER'
//LOWER <: T2 due to 'INITIALIZER'
//T0 <: T3 due to 'PARAMETER'
//T1 <: T3 due to 'PARAMETER'
//T2 <: T3 due to 'PARAMETER'
//T3 <: T4 due to 'INITIALIZER'
