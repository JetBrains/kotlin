// RUNTIME_WITH_FULL_JDK

fun main() {
    val list: /*T5@*/List</*T4@*/String?> =
        listOf</*T0@*/String?>(""/*LIT*/, null/*NULL!!U*/)/*List<T0@String>!!L*/
            .map</*T2@*/String?, /*T3@*/String?>({ x: /*T1@*/String? -> x/*T1@String*/ }/*Function1<T1@String, T6@String>!!L*/)/*List<T3@String>!!L*/
}

//LOWER <: T0 due to 'PARAMETER'
//UPPER <: T0 due to 'PARAMETER'
//T1 <: T6 due to 'RETURN'
//T0 <: T2 due to 'RECEIVER_PARAMETER'
//T2 <: T1 due to 'PARAMETER'
//T6 <: T3 due to 'PARAMETER'
//T3 <: T4 due to 'INITIALIZER'
//LOWER <: T5 due to 'INITIALIZER'
