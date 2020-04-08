// PROBLEM: none

class Test {
    companion object {

    }
}

fun <caret>Test.Companion.foo() = 42

fun main(args: Array<String>) {
    Test.foo()
}