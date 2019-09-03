// RUNTIME_WITH_FULL_JDK

import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream

fun test(list: /*T1@*/List</*T0@*/String>) {
    val x: /*T9@*/List</*T8@*/String> = list/*T1@List<T0@String>*/.stream()/*Stream<T0@String>!!L*/
        .map</*T3@*/String>({ x: /*T2@*/String -> x/*T2@String*/ + ""/*LIT*//*LIT*/ }/*Function1<T2@String, T10@String>!!L*/)/*Stream<T3@String>*/
        .collect</*T6@*/List</*T5@*/String>, /*T7@*/Any?>(Collectors/*LIT*/.toList</*T4@*/String>()/*Collector<T4@String, Any, MutableList<T4@String>>*/)/*T6@List<T5@String>*/

}

//T0 <: T0 due to 'RECEIVER_PARAMETER'
//T1 := LOWER due to 'USE_AS_RECEIVER'
//T2 := LOWER due to 'USE_AS_RECEIVER'
//LOWER <: T10 due to 'RETURN'
//T0 := T0 due to 'RECEIVER_PARAMETER'
//T0 <: T2 due to 'PARAMETER'
//T10 <: T3 due to 'PARAMETER'
//T3 := T3 due to 'RECEIVER_PARAMETER'
//T3 := T4 due to 'PARAMETER'
//T5 := T4 due to 'PARAMETER'
//T5 <: T8 due to 'INITIALIZER'
//T6 <: T9 due to 'INITIALIZER'
