package testing

fun foo() {
}

fun foo(a: Int) {
}

fun String.foo() {
}

fun main(args: Array<String>) {
    foo()
    foo(1)
    "".foo()
}