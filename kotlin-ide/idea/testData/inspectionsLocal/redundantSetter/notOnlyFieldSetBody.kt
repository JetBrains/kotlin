// PROBLEM: none
class Test {
    var x = 1
        <caret>set(value) {
            foo()
            field = value
        }

    fun foo() {}
}