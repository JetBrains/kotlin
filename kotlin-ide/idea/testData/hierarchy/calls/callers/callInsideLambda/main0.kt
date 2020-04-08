fun <caret>foo() = 1

fun bar() {
    val x = {
        val y = foo()
    }
}

fun baz() {
    bar()
}