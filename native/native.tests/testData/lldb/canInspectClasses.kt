// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectClasses.in
// OUTPUT_DATA_FILE: canInspectClasses.out



fun main(args: Array<String>) {
    val point = Point(1, 2)
    val person = Person()
    return
}

data class Point(val x: Int, val y: Int)
class Person {
    override fun toString() = "John Doe"
}
