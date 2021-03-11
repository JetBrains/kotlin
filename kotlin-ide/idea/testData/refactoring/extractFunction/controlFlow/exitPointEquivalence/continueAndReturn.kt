// SIBLING:
fun foo(a: Int) {
    val b: Int = 1
    for (n in 1..b) {
        <selection>if (a > 0) throw Exception("")
        if (a + b > 0) continue
        println(a - b)
        return</selection>
    }
}