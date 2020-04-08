// FLOW: OUT

val foo: Int
    get(): Int {
        return <caret>0
    }

fun test() {
    val x = foo
}