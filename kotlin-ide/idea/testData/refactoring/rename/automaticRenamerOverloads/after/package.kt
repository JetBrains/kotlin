package testing

fun bar() {
}

fun bar(a: Int) {
}

fun String.bar() {
}

fun main(args: Array<String>) {
    bar()
    bar(1)
    "".bar()
}