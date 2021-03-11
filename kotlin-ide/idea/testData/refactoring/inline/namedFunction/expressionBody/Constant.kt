fun <caret>f() = "foo"

fun main(args: Array<String>) {
    println(f() + f())
}