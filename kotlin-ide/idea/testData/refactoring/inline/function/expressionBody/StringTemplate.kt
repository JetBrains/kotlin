fun foo() {
    fun bar() = ""
    val y = "!!x=${<caret>bar()}!!"
}
