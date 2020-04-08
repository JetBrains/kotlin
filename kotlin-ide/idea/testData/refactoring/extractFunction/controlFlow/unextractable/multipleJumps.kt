// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <selection>if (a + b > 0) break
        println(a - b)
        if (a - b > 0) continue
        println(a + b)</selection>
    }
    return 1
}