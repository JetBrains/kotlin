// "Replace with 'newFun(this)'" "true"

open class C {
    @Deprecated("", ReplaceWith("newFun(this)"))
    fun oldFun(){}
}

fun newFun(c: C){}

class X {
    companion object : C() {
    }

    fun foo() {
        <caret>oldFun()
    }
}

