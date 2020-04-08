// SIBLING:
fun foo(a: Int): Int {
    object A {
        fun bar(): Int = a + 10
    }

    return <selection>A.bar()</selection>
}