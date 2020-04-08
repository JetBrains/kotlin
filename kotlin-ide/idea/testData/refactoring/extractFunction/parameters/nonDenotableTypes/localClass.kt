// SIBLING:
fun foo(a: Int): Int {
    class T(val b: Int)

    val t = T(1)
    return <selection>a + t.b</selection>
}