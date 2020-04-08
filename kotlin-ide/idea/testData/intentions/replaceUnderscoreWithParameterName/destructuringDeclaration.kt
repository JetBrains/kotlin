data class Data(val first: Int, val second: Int)

fun foo() {
    val (first, <caret>_) = Data(1, 2)
}