// PROBLEM: none
fun main() {
    "hello".foo()
}

fun bar(block: Int.() -> Unit) { block(42) }

fun String.foo() = bar {
    println("this: <caret>${this@foo}")
}

fun println(s: String) {}