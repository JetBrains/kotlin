// PROBLEM: none

class Test {
    companion object {
        fun memberFun(x: Int) = 1
    }

    fun memberFun(x: Int) = 2

    fun test() {
        <caret>Companion.memberFun(0)
    }
}