// WITH_RUNTIME

fun foo(x: String) {
    println(x)
}

fun main() {
    "doo".apply(::<caret>foo)
}