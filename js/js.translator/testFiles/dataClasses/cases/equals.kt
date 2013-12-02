package foo

data class TwoThings(val a: Int, val b: Char)

data class Bar(val a: Boolean,
               val b: Char,
               val c: Byte,
               val d: Short,
               val e: Int,
               val f: Float,
               val g: Long,
               val h: Double,
               val i: Array<TwoThings?>)

data class Baz(val a: Boolean,
               val b: Char,
               val c: Byte,
               val d: Short,
               val e: Int,
               val f: Float,
               val g: Long,
               val h: Double,
               val i: Array<TwoThings?>) {
    fun equals(that: Any): Boolean {
        if (that == null)
            return false
        if (that !is Baz)
            return false
        return this.a == that.a &&
            this.c == that.c &&
            this.d == that.d &&
            this.e == that.e &&
            this.f == that.f &&
            this.g == that.g &&
            this.h == that.h &&
            this.i == that.i
    }
}

fun box(): String {
    val lhs1 = Bar(true, 'a', 1, 2, 3, 4.5, 0xcafebabe, 4.2, array(TwoThings(5, 'z'), null))
    var rhs1 = lhs1.copy()
    if (rhs1 != lhs1)
        return "fail: rhs1 != lhs1"
    var rhs2 = lhs1.copy(b = 'z')
    if (rhs2 == lhs1)
        return "fail: rhs2 == lhs1"
    val lhs2 = Baz(true, 'a', 1, 2, 3, 4.5, 0xcafebabe, 4.2, array(TwoThings(5, 'z'), null))
    var rhs3 = lhs2.copy(b = 'z')
    if (lhs2 != rhs3)
        return "fail: lhs2 != rhs3"
    return "OK"
}