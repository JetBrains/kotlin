fun main() {
    val first = listOf("hello")
    val second = listOf("hello", "world")
    val result = second.any(<caret>first::contains)
    println(result)
}