fun test(a: /*T1@*/List</*T0@*/Int?>) {
    for (i: /*T2@*/Int? in a/*T1@List<T0@Int>*/) {

    }
}

//T0 <: T2 due to 'ASSIGNMENT'
//T1 := LOWER due to 'USE_AS_RECEIVER'
