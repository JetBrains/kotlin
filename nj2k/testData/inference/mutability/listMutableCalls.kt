fun a(
    l0: /*T1@*/MutableList</*T0@*/Int>,
    l1: /*T3@*/MutableList</*T2@*/Int>,
    l2: /*T5@*/MutableList</*T4@*/Int>,
    l3: /*T7@*/MutableList</*T6@*/Int>,
    l4: /*T9@*/MutableList</*T8@*/Int>,
    l5: /*T11@*/MutableList</*T10@*/Int>,
    l6: /*T13@*/MutableList</*T12@*/Int>,
    l7: /*T15@*/MutableList</*T14@*/Int>
) {
    l0/*T1@MutableList<T0@Int>*/.add(1/*LIT*/)
    l1/*T3@MutableList<T2@Int>*/.addAll(1/*LIT*/, l1/*T3@MutableList<T2@Int>*/)
    l2/*T5@MutableList<T4@Int>*/.addAll(l1/*T3@MutableList<T2@Int>*/)
    l3/*T7@MutableList<T6@Int>*/.clear()
    l4/*T9@MutableList<T8@Int>*/.remove(1/*LIT*/)
    l5/*T11@MutableList<T10@Int>*/.removeAll(l1/*T3@MutableList<T2@Int>*/)
    l5/*T11@MutableList<T10@Int>*/.removeAt(0/*LIT*/)
    l6/*T13@MutableList<T12@Int>*/.retainAll(l1/*T3@MutableList<T2@Int>*/)
    l7/*T15@MutableList<T14@Int>*/.set(0/*LIT*/, 0/*LIT*/)
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T2 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T3 := LOWER due to 'USE_AS_RECEIVER'
//T4 := T4 due to 'RECEIVER_PARAMETER'
//T4 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T5 := LOWER due to 'USE_AS_RECEIVER'
//T6 := T6 due to 'RECEIVER_PARAMETER'
//T7 := LOWER due to 'USE_AS_RECEIVER'
//T8 := T8 due to 'RECEIVER_PARAMETER'
//T9 := LOWER due to 'USE_AS_RECEIVER'
//T10 := T10 due to 'RECEIVER_PARAMETER'
//T10 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T11 := LOWER due to 'USE_AS_RECEIVER'
//T10 := T10 due to 'RECEIVER_PARAMETER'
//T12 := T12 due to 'RECEIVER_PARAMETER'
//T12 := T2 due to 'PARAMETER'
//T3 <: UPPER due to 'PARAMETER'
//T13 := LOWER due to 'USE_AS_RECEIVER'
//T14 := T14 due to 'RECEIVER_PARAMETER'
//T15 := LOWER due to 'USE_AS_RECEIVER'
