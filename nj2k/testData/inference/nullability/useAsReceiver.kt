fun test() {
    val x: /*T2@*/List</*T1@*/Any> = listOf</*T0@*/Any>()/*List<T0@Any>!!L*/
    x/*T2@List<T1@Any>*/.get(0/*LIT*/)
}

//T0 <: T1 due to 'INITIALIZER'
//LOWER <: T2 due to 'INITIALIZER'
//T1 <: T1 due to 'RECEIVER_PARAMETER'
//T2 := LOWER due to 'USE_AS_RECEIVER'
