// PROBLEM: none

class Something {
    fun nullable(): Int? = null
}
fun Something?.nullable(value: Int): Int? =
    <caret>if (this == null) value else nullable()