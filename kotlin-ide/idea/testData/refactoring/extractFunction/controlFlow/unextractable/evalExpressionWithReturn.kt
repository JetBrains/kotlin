// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1

    val t = <selection>if (a > 0) return a else a + b</selection>
    return t
}