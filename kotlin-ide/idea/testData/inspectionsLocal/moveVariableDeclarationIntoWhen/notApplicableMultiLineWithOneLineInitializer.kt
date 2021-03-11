// PROBLEM: none
fun test() = 42

fun foo() {
    val <caret>a =
        test()
    when (a) {
        1 -> a
        else -> 24
    }
}