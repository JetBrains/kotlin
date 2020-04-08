// FLOW: IN

val foo: Int
    get() = 0

fun test() {
    val <caret>x = foo
}