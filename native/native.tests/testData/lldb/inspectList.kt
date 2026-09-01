// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectList.in
// OUTPUT_DATA_FILE: inspectList.out



fun main(args: Array<String>) {
    val list: List<Point> = listOf(Point(1, 2), Point(3, 4))
    val intList: List<Int> = listOf(1, 2, 3)
    val mutableList: MutableList<Point> = mutableListOf(Point(8, 9), Point(10, 11))
    val emptyList: List<Point> = emptyList()
    val nestedList: List<List<Point>> =
        listOf(listOf(Point(4, 5), Point(6, 7)), emptyList())
    return
}

data class Point(val x: Int, val y: Int)
