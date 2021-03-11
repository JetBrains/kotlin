// PROBLEM: none

fun test(n: Int) {
    val a: String
    val b: String
    <caret>if (n == 1)
        a = "one"
    else if (n == 2)
        a = "two"
    else
        b = "three"
}