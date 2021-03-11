data class A(val <caret>x: Int)

fun main(args: Array<String>) {
    val a = A(x = 1)
    val b = a.copy(x = 2)
}
