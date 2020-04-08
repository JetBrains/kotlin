// WITH_RUNTIME

infix fun Int.upTo(other: Int) = this.rangeTo(other)

fun main() {
    (1 upTo 2).<caret>forEach { x -> x }
}