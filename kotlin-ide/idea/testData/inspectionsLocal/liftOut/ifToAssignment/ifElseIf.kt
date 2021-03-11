// PROBLEM: none

fun test(n: Int) {
    var a: String = ""
    <caret>if (n == 1)
        a = "one"
    else if (n == 2)
        a = "two"
}