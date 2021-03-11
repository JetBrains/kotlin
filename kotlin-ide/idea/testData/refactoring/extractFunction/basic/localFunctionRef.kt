// SIBLING:
fun foo(a: Int): Int {
    fun bar(): Int = a + 10

    return <selection>bar()</selection>
}