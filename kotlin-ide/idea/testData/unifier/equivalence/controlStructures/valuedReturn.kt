// DISABLE-ERRORS
fun foo(a: Int): Int {
    if (a > 0) <selection>return (a) + 1</selection>
    if (a < 0) return
    if (a == 0) return a
    return (a + 1)
    return
}