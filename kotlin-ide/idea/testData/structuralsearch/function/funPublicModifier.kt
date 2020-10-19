<warning descr="SSR">public fun foo1() { }</warning>

val eps = 1E-10 // "good enough", could be 10^-15

<warning descr="SSR">public tailrec fun findFixPoint(x: Double = 1.0): Double
        = if (Math.abs(x - Math.cos(x)) < eps) x else findFixPoint(Math.cos(x))</warning>

fun bar() { }