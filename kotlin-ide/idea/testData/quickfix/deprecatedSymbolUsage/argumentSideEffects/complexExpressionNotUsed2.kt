// "Replace with 'newFun()'" "true"

class C {
    @Deprecated("", ReplaceWith("newFun()"))
    fun oldFun() {
        newFun()
    }
}

fun newFun(){}

fun foo() {
    getC().<caret>oldFun()
}

fun getC(): C = C()
