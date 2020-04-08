// WITH_DEFAULT_VALUE: false
public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class A(val a: Int) {
    fun A.foo(): Int {
        return (<selection>this@A.a + a</selection>) / 2
    }

    fun test() {
        A(1).foo()
    }
}

fun test() {
    val t = with(A(1)) {
        A(2).foo()
    }
}