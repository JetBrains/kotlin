package test

fun foo(a: A): Int = when (a) {
    is B -> 1
    is C -> 2
}
