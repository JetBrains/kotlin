// OPTION: 0
fun foo(n: Int): Int {
    return <caret>if (n > 0) {
        n + 10
    } else {
        n - 10
    }
}
