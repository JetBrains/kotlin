package test

expect fun foo(s: String, vararg <caret>n: Int)

fun test() {
    foo("1", 2, 3)
}