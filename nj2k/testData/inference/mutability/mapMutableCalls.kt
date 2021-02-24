fun a(
    l0: /*T2@*/MutableMap</*T0@*/Int, /*T1@*/String>,
    l1: /*T5@*/MutableMap</*T3@*/Int, /*T4@*/String>,
    l2: /*T8@*/MutableMap</*T6@*/Int, /*T7@*/String>,
    l3: /*T11@*/MutableMap</*T9@*/Int, /*T10@*/String>
) {
    l0/*T2@MutableMap<T0@Int, T1@String>*/.put(1)
    l1/*T5@MutableMap<T3@Int, T4@String>*/.remove(1/*LIT*/, l1)
    l2/*T8@MutableMap<T6@Int, T7@String>*/.putAll(l1/*T5@MutableMap<T3@Int, T4@String>*/)
    l3/*T11@MutableMap<T9@Int, T10@String>*/.clear(1)
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := T1 due to 'RECEIVER_PARAMETER'
//T2 := LOWER due to 'USE_AS_RECEIVER'
//T3 := T3 due to 'RECEIVER_PARAMETER'
//T4 := T4 due to 'RECEIVER_PARAMETER'
//T5 := LOWER due to 'USE_AS_RECEIVER'
//T6 := T6 due to 'RECEIVER_PARAMETER'
//T7 := T7 due to 'RECEIVER_PARAMETER'
//T6 := T3 due to 'PARAMETER'
//T7 := T4 due to 'PARAMETER'
//T5 <: UPPER due to 'PARAMETER'
//T8 := LOWER due to 'USE_AS_RECEIVER'
//T9 := T9 due to 'RECEIVER_PARAMETER'
//T10 := T10 due to 'RECEIVER_PARAMETER'
//T11 := LOWER due to 'USE_AS_RECEIVER'
