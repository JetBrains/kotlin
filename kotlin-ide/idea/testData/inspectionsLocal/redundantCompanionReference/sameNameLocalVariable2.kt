// PROBLEM: none

class Test {
    companion object {
        val localVar = 1
    }

    fun test(localVar: Int) {
        <caret>Companion.localVar
    }
}