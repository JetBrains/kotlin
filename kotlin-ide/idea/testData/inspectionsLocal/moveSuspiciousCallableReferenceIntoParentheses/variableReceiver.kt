// WITH_RUNTIME
class Test {
    val lambda = { s: String -> true }
    fun test() {
        "".let {<caret> lambda::invoke }
    }
}
