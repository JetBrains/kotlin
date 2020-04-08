// WITH_DEFAULT_VALUE: false
fun foo(a: Int): Int {
    val b = 1
    return (<selection>a + b</selection>) * 2
}

fun test() {
    foo(1)
}