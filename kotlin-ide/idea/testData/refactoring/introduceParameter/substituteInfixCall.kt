// WITH_DEFAULT_VALUE: false
fun foo(a: Int): Int {
    fun Int.bar(n: Int) = this + n

    return (<selection>a bar 1</selection>) * 2
}

fun test() {
    foo(2)
}