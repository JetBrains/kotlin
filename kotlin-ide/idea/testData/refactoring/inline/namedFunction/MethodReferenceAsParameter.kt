fun main() {
    "foo".let(::foo)
}

fun <caret>foo(x: String) {
    println(x)
    println(42)
    x.let { "$it" }
}
