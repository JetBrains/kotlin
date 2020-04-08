fun println(s: String) {}

fun foo(y: Boolean) {
    <caret>if (!y) return
    println("no1")
    return
}