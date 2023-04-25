// EXPECTED_REACHABLE_NODES: 1376
// MODULE: lib
// FILE: lib.kt

inline fun foo(i1: Int, i2: Double): Int {
    val o = object : Comparable<Int> {
        override fun compareTo(other: Int) = i1-other
    }

    return o.compareTo(i2.toInt())
}

// MODULE: main(lib)
// FILE: main.kt

fun bar(i: Int):Int {
    class Cmp2() : Comparable<Int> {
        override fun compareTo(other: Int) = -other
    }

    return Cmp2().compareTo(i)
}

fun box():String {
    if (foo(10, 20.0) + bar(-10) != 0) return "FAIL"
    return "OK"
}
