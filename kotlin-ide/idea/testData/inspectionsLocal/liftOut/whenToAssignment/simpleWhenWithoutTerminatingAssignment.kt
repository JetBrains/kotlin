// PROBLEM: none
fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

    <caret>when (n) {
            1 -> {
                doSomething("***")
                res = "one"
            }
            else -> {
                res = "two"
                doSomething("***")
            }
    }

    return res
}
