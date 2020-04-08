// OPTION: 3
fun foo(n: Int): Int {
    if (n > 0) {
        println("> 0")
        n + 10
    } <caret>else {
        println("<= 0")
        n - 10
    }

    return n
}
