import java.util.ArrayList

fun a() {
    val list: /*T4@*/List</*T3@*/MutableList</*T2@*/Int>> = ArrayList</*T1@*/MutableList</*T0@*/Int>>()/*ArrayList<T1@MutableList<T0@Int>>*/
    list/*T4@MutableList<T3@MutableList<T2@Int>>*/.get(0/*LIT*/)/*T3@MutableList<T2@Int>*/.add(1/*LIT*/)
}

//T2 := T0 due to 'INITIALIZER'
//T3 := T1 due to 'INITIALIZER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T3 := T3 due to 'RECEIVER_PARAMETER'
//T4 <: UPPER due to 'RECEIVER_PARAMETER'
//T2 := T2 due to 'RECEIVER_PARAMETER'
//T3 := LOWER due to 'USE_AS_RECEIVER'
