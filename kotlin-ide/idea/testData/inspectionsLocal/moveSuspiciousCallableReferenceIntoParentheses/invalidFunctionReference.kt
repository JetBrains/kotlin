// WITH_RUNTIME
// FIX: none
fun test(x: Int) {
    fun p(s: String): Boolean = true
    x.let {<caret> ::p }
}