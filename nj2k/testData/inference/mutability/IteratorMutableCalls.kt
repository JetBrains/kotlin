fun a(
    l0: /*T1@*/MutableIterator</*T0@*/Int>
) {
    l0/*T1@MutableIterator<T0@Int>*/.remove()
}

//T0 <: T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
