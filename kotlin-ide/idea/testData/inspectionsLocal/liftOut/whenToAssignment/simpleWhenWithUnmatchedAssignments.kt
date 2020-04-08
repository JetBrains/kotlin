// PROBLEM: none
fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String = ""
    var res2: String = ""

    <caret>when (n) {
        1 -> {
            doSomething("***")
            res = "one"
        }
        else -> {
            doSomething("***")
            res2 = "two"
        }
    }

    return res + res2
}
