fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

    <caret>res = when (n) {
        1 -> {
            doSomething("***")
            "one"
        }
        else -> {
            doSomething("***")
            "two"
        }
    }

    return res
}
