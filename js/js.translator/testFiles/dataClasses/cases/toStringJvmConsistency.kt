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

fun box(): String {
    val str = Bar(true, 'a', 1, 2, 3, 4.5, 0xcafebabe, 4.2, array(TwoThings(5, 'z'), null)).toString()
    if (str == "Bar(a=true, b=a, c=1, d=2, e=3, f=4.5, g=3405691582, h=4.2, i=[TwoThings(a=5, b=z), null])")
        return "OK"
    return str
}
