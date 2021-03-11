package foo

class <caret>A {
    fun test() {
        A.testCompanion()
        testCompanion()
        Companion.testCompanion()
    }

    companion object {
        fun testCompanion() {

        }
    }
}