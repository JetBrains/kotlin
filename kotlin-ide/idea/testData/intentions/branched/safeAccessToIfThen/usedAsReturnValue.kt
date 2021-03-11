fun doSth(): Int? {
    val x: String? = "abc"
    return x?.<caret>length
}
fun main(args: Array<String>) {
    val y = doSth()
}
