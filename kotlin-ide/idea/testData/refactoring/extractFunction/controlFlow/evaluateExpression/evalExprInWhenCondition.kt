// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return when (a + b) {
        <selection>0</selection> -> b
        1 -> -b
        else -> a - b
    }
}
