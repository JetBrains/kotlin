fun main(args: Array<String>) {
    val x: String? = null
    val y: String? = "Hello"
    val z = (x ?: y)?.<caret>length
}
