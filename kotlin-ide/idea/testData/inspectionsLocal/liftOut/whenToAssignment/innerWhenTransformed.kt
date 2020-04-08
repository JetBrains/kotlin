fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

    if (3 > 2) {
        <caret>when (n) {
            1 -> {
                doSomething("***")
                res = "one"
            }
            else -> {
                doSomething("***")
                res = "two"
            }
        }
    } else {
        doSomething("***")
        res = "???"
    }

    return res
}
