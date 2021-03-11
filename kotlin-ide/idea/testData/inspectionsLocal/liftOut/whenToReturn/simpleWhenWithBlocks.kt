fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    <caret>when (n) {
        1 -> {
            doSomething("***")
            return "one"
        }
        else -> {
            doSomething("***")
            return "two"
        }
    }
}
