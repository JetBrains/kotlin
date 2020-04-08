fun <T> String.ext(): List<T> = listOf()

fun f() {
    val v : List<Int> = ("".ext())
    val vv = <caret>v
}
