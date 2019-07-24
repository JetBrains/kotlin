fun bar(map: /*T2@*/HashMap</*T0@*/String?, /*T1@*/Int>, list1: /*T4@*/List</*T3@*/Int>, list2: /*T6@*/List</*T5@*/String?>) {
    for (entry: /*T9@*/MutableMap.MutableEntry</*T7@*/String?, /*T8@*/Int> in map/*T2@HashMap<T0@String, T1@Int>*/.entries/*MutableSet<MutableEntry<T0@String, T1@Int>>*/) {
        val value: /*T10@*/Int = entry/*T9@MutableEntry<T7@String, T8@Int>*/.value/*T8@Int*/
        if (entry/*T9@MutableEntry<T7@String, T8@Int>*/.key/*T7@String*/ == null/*LIT*/) {
            println(value/*T10@Int*/ + 1/*LIT*//*LIT*/)
        }
    }

    for (i: /*T11@*/Int in list1/*T4@List<T3@Int>*/) {
        i/*T11@Int*/ + 1/*LIT*/
    }

    for (i: /*T12@*/String? in list2/*T6@List<T5@String>*/) {
        i/*T12@String*/ == null
    }
}

//T2 := LOWER due to 'USE_AS_RECEIVER'
//T9 := LOWER due to 'USE_AS_RECEIVER'
//T8 <: T10 due to 'INITIALIZER'
//T9 := LOWER due to 'USE_AS_RECEIVER'
//T7 := UPPER due to 'COMPARE_WITH_NULL'
//T10 := LOWER due to 'USE_AS_RECEIVER'
//T0 := T7 due to 'ASSIGNMENT'
//T1 := T8 due to 'ASSIGNMENT'
//T11 := LOWER due to 'USE_AS_RECEIVER'
//T11 <: T3 due to 'ASSIGNMENT'
//T4 := LOWER due to 'USE_AS_RECEIVER'
//T12 := UPPER due to 'COMPARE_WITH_NULL'
//T12 <: T5 due to 'ASSIGNMENT'
//T6 := LOWER due to 'USE_AS_RECEIVER'
