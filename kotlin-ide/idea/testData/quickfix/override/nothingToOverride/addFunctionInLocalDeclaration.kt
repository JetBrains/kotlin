// "Add 'open fun f()' to 'A'" "true"
open class A {
}

fun test() {
    val some = object : A() {
        <caret>override fun f() {}
    }
}