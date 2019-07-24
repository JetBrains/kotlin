fun test() {
    val x: /*T2@*/X</*T1@*/Int> = X</*T0@*/Int>()/*X<T0@Int>*/
    val y: /*T5@*/Map</*T3@*/Int, /*T4@*/String> = x/*T2@X<T1@Int>*/.foo()/*T8@Map<T1@Int, T7@String>*/
}

class X<T> {
    fun foo(): /*T8@*/Map</*T6@*/T, /*T7@*/String> {
        TODO()
    }
}

//T1 := T0 due to 'INITIALIZER'
//T1 := T1 due to 'RECEIVER_PARAMETER'
//T3 := T1 due to 'INITIALIZER'
//T7 <: T4 due to 'INITIALIZER'
//T8 <: T5 due to 'INITIALIZER'
