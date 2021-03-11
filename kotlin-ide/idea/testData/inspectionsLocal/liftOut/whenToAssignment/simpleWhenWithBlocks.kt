fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

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

    return res
}
