// WITH_DEFAULT_VALUE: false
// TARGET:
class Foo(val a: Int) {
    fun foo(n: Int): Int {
        val t = a + n + 1
        return <selection>a - n</selection> - t
    }
}

fun test() {
    Foo(1).foo(2)
}