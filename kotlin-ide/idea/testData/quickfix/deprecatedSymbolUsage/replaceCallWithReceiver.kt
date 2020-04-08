// "Replace with 'this'" "true"
class C {
    @Deprecated("", ReplaceWith("this"))
    fun oldFun(): C = this
}

fun foo() {
    C().<caret>oldFun()
}
