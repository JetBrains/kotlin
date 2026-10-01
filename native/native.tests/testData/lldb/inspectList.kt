// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectList.in
// OUTPUT_DATA_FILE: inspectList.out



fun main(args: Array<String>) {
    val list: List<Point> = listOf(Point(1, 2), Point(3, 4))
    val intList: List<Int> = listOf(1, 2, 3)
    val mutableList: MutableList<Point> = mutableListOf(Point(8, 9), Point(10, 11))
    val emptyList: List<Point> = emptyList()
    val subList: List<Point> = arrayListOf(
        Point(0, 0), Point(1, 1), Point(2, 2), Point(3, 3)
    ).subList(1, 3)
    val nestedSubList: List<Point> = subList.subList(1, 2)
    val nestedList: List<List<Point>> =
        listOf(listOf(Point(4, 5), Point(6, 7)), emptyList())
    return
}

data class Point(val x: Int, val y: Int)
