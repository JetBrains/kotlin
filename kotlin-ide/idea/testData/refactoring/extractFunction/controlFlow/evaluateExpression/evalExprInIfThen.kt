// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return if (a + b > 0) <selection>1</selection> else if (a - b < 0) 2 else b
}
