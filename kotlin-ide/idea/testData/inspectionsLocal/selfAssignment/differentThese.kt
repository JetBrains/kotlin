// PROBLEM: none
// WITH_RUNTIME

class Test {
    var foo = 1

    fun test() {
        with (Test()) {
            this@Test.foo = <caret>this.foo // Different receivers
        }
    }
}