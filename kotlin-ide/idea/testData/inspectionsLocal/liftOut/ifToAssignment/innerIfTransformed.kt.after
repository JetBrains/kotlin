fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

    if (n == 1) {
        <caret>res = if (3 > 2) {
            doSomething("***")
            "one"
        } else {
            doSomething("***")
            "???"
        }
    } else if (n == 2) {
        doSomething("***")
        res = "two"
    } else {
        doSomething("***")
        res = "too many"
    }

    return res
}
