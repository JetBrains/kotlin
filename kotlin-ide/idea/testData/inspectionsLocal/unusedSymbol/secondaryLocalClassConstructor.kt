// PROBLEM: none
// WITH_RUNTIME

fun main() {
    class LocalClass(val number: Int) {
        <caret>constructor(s: String) : this(s.toInt())
    }

    val l = LocalClass("42")
}