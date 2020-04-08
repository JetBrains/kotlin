// WITH_DEFAULT_VALUE: false
class T(val t: Int)

fun foo(a: Int): Int {
    return <selection>T(a + 1)</selection>.t / 2
}

fun bar(x: Int = foo(T(2).t))

fun test() {
    foo(T(2).t)
}