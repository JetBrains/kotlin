// PROBLEM: none

class Test {
    companion object {
        val extentionVar = 1
    }

    fun test() {
        <caret>Companion.extentionVar
    }
}

val Test.extentionVar: Int
    get() = 2