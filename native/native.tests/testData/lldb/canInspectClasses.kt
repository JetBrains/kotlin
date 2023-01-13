// KIND: STANDALONE_LLDB
// LLDB_TRACE: canInspectClasses.txt
fun main(args: Array<String>) {
    val point = Point(1, 2)
    val person = Person()
    return
}

data class Point(val x: Int, val y: Int)
class Person {
    override fun toString() = "John Doe"
}
