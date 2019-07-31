fun a(lst: /*T1@*/List</*T0@*/String>) {
    val newList: /*T5@*/List</*T4@*/String> = lst/*T1@List<T0@String>*/
        .asSequence</*T2@*/String>()/*Sequence<T2@String>*/
        .toList</*T3@*/String>()/*List<T3@String>*/
}

//T0 <: T2 due to 'RECEIVER_PARAMETER'
//T2 <: T3 due to 'RECEIVER_PARAMETER'
//T3 <: T4 due to 'INITIALIZER'
