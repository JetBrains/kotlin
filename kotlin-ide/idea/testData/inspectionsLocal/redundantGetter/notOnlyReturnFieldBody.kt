// PROBLEM: none
class Test {
    val x = 1
        <caret>get() {
            foo()
            return field
        }

    fun foo() {}
}