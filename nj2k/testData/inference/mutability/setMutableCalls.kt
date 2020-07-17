fun a(
    l0: /*T1@*/MutableSet</*T0@*/Int>,
    l1: /*T3@*/MutableSet</*T2@*/Int>,
    l2: /*T5@*/MutableSet</*T4@*/Int>,
    l3: /*T7@*/MutableSet</*T6@*/Int>,
    l4: /*T9@*/MutableSet</*T8@*/Int>
) {
    l0/*T1@MutableSet<T0@Int>*/.add(1/*LIT*/)
    l1/*T3@MutableSet<T2@Int>*/.addAll(l1/*T3@MutableSet<T2@Int>*/)
    l2/*T5@MutableSet<T4@Int>*/.clear()
    l3/*T7@MutableSet<T6@Int>*/.removeAll(l1/*T3@MutableSet<T2@Int>*/)
    l4/*T9@MutableSet<T8@Int>*/.retainAll(l1/*T3@MutableSet<T2@Int>*/)
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T2 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T3 := LOWER due to 'USE_AS_RECEIVER'
//T4 := T4 due to 'RECEIVER_PARAMETER'
//T5 := LOWER due to 'USE_AS_RECEIVER'
//T6 := T6 due to 'RECEIVER_PARAMETER'
//T6 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T7 := LOWER due to 'USE_AS_RECEIVER'
//T8 := T8 due to 'RECEIVER_PARAMETER'
//T8 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T9 := LOWER due to 'USE_AS_RECEIVER'
