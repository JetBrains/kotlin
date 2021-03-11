// IS_APPLICABLE: false
fun foo(n: Int): Int {
    return if (n > 0) {
        println("> 0")
        n + 10
    } <caret>else {
        println("<= 0")
        n - 10
    }
}
