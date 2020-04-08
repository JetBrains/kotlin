// FLOW: OUT

val foo: Int
    get() = <caret>0

fun test() {
    val x = foo
}