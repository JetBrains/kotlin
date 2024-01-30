// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
fun main(args: Array<String>) {
    val point = Point(1, 2)
    val person = Person()
    return
}

data class Point(val x: Int, val y: Int)
class Person {
    override fun toString() = "John Doe"
}
