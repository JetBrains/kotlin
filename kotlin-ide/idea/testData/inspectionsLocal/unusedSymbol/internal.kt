// PROBLEM: none

class Foo {
    fun test() {
        abacaba("")
    }

    internal fun <caret>abacaba(s: String): String = s
}
