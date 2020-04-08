// PROBLEM: none
fun bar(): Int = 1

fun test() : Int? {
    return bar() <caret>?: return null // USELESS_ELVIS
}