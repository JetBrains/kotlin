// WITH_DEFAULT_VALUE: false
fun foo(vararg a: Int): Int {
    return (<selection>a.size + 1</selection>) * 2
}

fun test() {
    foo()
    foo(1)
    foo(1, 2)
}