// "Remove argument" "true"
class Bar(s: String, i: Int) {
    fun foo(s: String) {
    }
}

fun main() {
    val b = Bar("2", 1)
    b.foo("a", 1<caret>)
}