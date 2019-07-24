fun a(lst: /*T1@*/List</*T0@*/String>) {
    val newList: /*T8@*/List</*T7@*/Int> = lst/*T1@List<T0@String>*/
        .asSequence</*T2@*/String>()/*Sequence<T2@String>!!L*/
        .map</*T4@*/String, /*T5@*/Int>({ x: /*T3@*/String -> 1/*LIT*/ }/*Function1<T3@String, T9@Int>!!L*/)/*Sequence<T5@Int>!!L*/
        .toList</*T6@*/Int>()/*List<T6@Int>!!L*/
}

//T0 <: T2 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
//LOWER <: T9 due to 'RETURN'
//T2 <: T4 due to 'RECEIVER_PARAMETER'
//T4 <: T3 due to 'PARAMETER'
//T9 <: T5 due to 'PARAMETER'
//T5 <: T6 due to 'RECEIVER_PARAMETER'
//T6 <: T7 due to 'INITIALIZER'
//LOWER <: T8 due to 'INITIALIZER'
