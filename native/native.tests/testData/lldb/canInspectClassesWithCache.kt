// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectClassesWithCache.in
// OUTPUT_DATA_FILE: canInspectClassesWithCache.out



fun main(args: Array<String>) {
    val point = Point(1, 2)
    val person = Person()
    return
}

data class Point(val x: Int, val y: Int)
class Person {
    override fun toString() = "John Doe"
}
