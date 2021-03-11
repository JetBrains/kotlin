// WITH_RUNTIME
// PROBLEM: none

class Foo {
    var s = ""

    fun test() {
        val s = ""
        <caret>this.s = s
    }
}