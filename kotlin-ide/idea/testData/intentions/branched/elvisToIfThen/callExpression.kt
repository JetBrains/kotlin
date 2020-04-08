fun foo(): String? {
    return "foo"
}

fun bar() {
}

fun main(args: Array<String>) {
    foo() ?:<caret> bar()
}
