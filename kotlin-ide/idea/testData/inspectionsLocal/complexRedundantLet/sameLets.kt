// PROBLEM: none
// WITH_RUNTIME


fun foo() {
    val foo: String? = null
    foo?.toString()?.let<caret> {
        it.to("")
    }?.let {
        it.to("")
    }
}