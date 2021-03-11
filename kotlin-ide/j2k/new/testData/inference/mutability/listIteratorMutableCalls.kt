fun a(
    l0: /*T1@*/MutableListIterator</*T0@*/Int>,
    l1: /*T3@*/MutableListIterator</*T2@*/Int>,
    l2: /*T5@*/MutableListIterator</*T4@*/Int>
) {
    l0/*T1@MutableListIterator<T0@Int>*/.add(1/*LIT*/)
    l1/*T3@MutableListIterator<T2@Int>*/.remove()
    l2/*T5@MutableListIterator<T4@Int>*/.set(1/*LIT*/)
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T3 := LOWER due to 'USE_AS_RECEIVER'
//T4 := T4 due to 'RECEIVER_PARAMETER'
//T5 := LOWER due to 'USE_AS_RECEIVER'
