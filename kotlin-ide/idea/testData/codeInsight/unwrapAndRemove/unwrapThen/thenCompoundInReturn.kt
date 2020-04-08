// IS_APPLICABLE: false
fun foo(n: Int): Int {
    return <caret>if (n > 0) {
        println("> 0")
        n + 10
    } else {
        println("<= 0")
        n - 10
    }
}
