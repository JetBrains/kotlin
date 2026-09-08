// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectArray.in
// OUTPUT_DATA_FILE: inspectArray.out



fun main(args: Array<String>) {
    val array: Array<Point> = arrayOf(Point(1, 2), Point(3, 4))
    val emptyArray: Array<Point> = emptyArray()
    val boxedIntArray: Array<Int> = arrayOf(1, 2, 3)
    val nestedArray: Array<Array<Array<Point>>> =
        arrayOf(arrayOf(arrayOf(Point(4, 5), Point(6, 7))), emptyArray())
    return
}

data class Point(val x: Int, val y: Int)
