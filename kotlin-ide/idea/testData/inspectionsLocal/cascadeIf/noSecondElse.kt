// PROBLEM: none

fun println(s: String) {}

fun test(a: Boolean, b: Boolean) {
    <caret>if (a) {
        println("a")
    }
    else if (b) {
        println("b")
    }
}