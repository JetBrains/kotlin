// PROBLEM: none

class Test {
    companion object {
        fun extentionFun() = 1
    }

    fun test() {
        <caret>Companion.extentionFun()
    }
}

fun Test.extentionFun() = 2