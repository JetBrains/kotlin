package test

fun f(s: S): Int = when (s) {
    is A -> 1
    is B -> 2
    else -> 0
}
