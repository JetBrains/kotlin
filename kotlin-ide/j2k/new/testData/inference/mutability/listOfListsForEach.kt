import java.util.ArrayList

fun test() {
    val list: /*T4@*/List</*T3@*/MutableList</*T2@*/Int>> = ArrayList</*T1@*/MutableList</*T0@*/Int>>()/*ArrayList<T1@MutableList<T0@Int>>*/
    for(l: /*T6@*/MutableList</*T5@*/Int> in list/*T4@MutableList<T3@MutableList<T2@Int>>*/) {
        l/*T6@MutableList<T5@Int>*/.add(42/*LIT*/)
    }
}

//T2 := T0 due to 'INITIALIZER'
//T3 := T1 due to 'INITIALIZER'
//T5 := T5 due to 'RECEIVER_PARAMETER'
//T6 := LOWER due to 'USE_AS_RECEIVER'
//T5 := T2 due to 'ASSIGNMENT'
//T3 <: T6 due to 'ASSIGNMENT'
