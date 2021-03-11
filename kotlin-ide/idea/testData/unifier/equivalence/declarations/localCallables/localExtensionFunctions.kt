// DISABLE-ERRORS
class A(val n: Int)

fun test() {
    <selection>fun <T: A> foo(t: T): Int {
        fun A.a(n: Int): Int = this.n + n
        fun A.b(n: Int): Int = this.n - n

        return t.n + A(1).a(2) - A(2).b(1)
    }</selection>

    fun <U: A> foo(u: U): Int {
        fun A.x(m: Int): Int = n + m
        fun A.y(n: Int): Int = this.n - n

        return u.n + A(1).x(2) - A(2).y(1)
    }

    fun <V: A> foo(v: V): Int {
        fun A.a(n: Int): Int = this.n + n
        fun A.b(n: Int): Int = this.n + n

        return v.n + A(1).a(2) - A(2).b(1)
    }
}