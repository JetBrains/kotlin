// WITH_DEFAULT_VALUE: false

fun foo(a: Int, b: Int): Int {
    return <selection>a * b</selection> / 2
}

fun test() {
    foo(1.plus(2), 3.minus(4))
}