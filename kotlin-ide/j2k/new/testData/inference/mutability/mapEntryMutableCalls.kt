fun a(
    l0: /*T2@*/MutableMap.MutableEntry</*T0@*/Int, /*T1@*/String>
) {
    l0/*T2@MutableEntry<T0@Int, T1@String>*/.setValue(1/*LIT*/, "nya")
}

//T0 := T0 due to 'RECEIVER_PARAMETER'
//T1 := T1 due to 'RECEIVER_PARAMETER'
//T2 := LOWER due to 'USE_AS_RECEIVER'
