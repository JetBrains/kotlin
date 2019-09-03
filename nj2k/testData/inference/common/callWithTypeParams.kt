fun test() {
    foo</*T1@*/Int>(
        listOf</*T0@*/Int>()/*List<T0@Int>*/
    )
}

fun <T> foo (x: /*T3@*/List</*T2@*/T>) {}

//T0 <: T1 due to 'PARAMETER'
