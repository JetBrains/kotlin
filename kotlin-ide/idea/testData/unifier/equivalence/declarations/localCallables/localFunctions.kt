// DISABLE-ERRORS
class A(val n: Int)

fun test() {
    <selection>fun <T: A> foo(t: T): Int {
        fun a(p: Int): Int = p + 1
        fun b(q: Int): Int = q - 1

        return t.n + a(1) - b(2)
    }</selection>

    fun <U: A> foo(u: U): Int {
        fun x(a: Int): Int = a + 1
        fun y(a: Int): Int = a - 1

        return u.n + x(1) - y(2)
    }

    fun <V: A> foo(v: V): Int {
        fun a(p: Int): Int = p + 1
        fun b(p: Int): Int = p + 1

        return v.n + a(1) - b(2)
    }

    fun a(p: Int): Int = p + 1
    fun b(q: Int): Int = q - 1

    fun <W: A> foo(w: W): Int {
        return w.n + a(1) - b(2)
    }
}