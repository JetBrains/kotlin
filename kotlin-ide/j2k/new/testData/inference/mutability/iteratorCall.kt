fun a(l0: /*T1@*/MutableList</*T0@*/Int>) {
    val iterator: /*T3@*/MutableIterator</*T2@*/Int> = l0/*T1@MutableList<T0@Int>*/.iterator()/*T1@MutableList<T0@Int>*/
    iterator/*T3@MutableIterator<T2@Int>*/.remove()
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T2 := T0 due to 'INITIALIZER'
//T1 <: T3 due to 'INITIALIZER'
//T2 <: T2 due to 'RECEIVER_PARAMETER'
//T3 := LOWER due to 'USE_AS_RECEIVER'
