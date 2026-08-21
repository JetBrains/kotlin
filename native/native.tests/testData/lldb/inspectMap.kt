// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectMap.in
// OUTPUT_DATA_FILE: inspectMap.out



fun main(args: Array<String>) {
    val map: Map<Int, Point> = linkedMapOf(10 to Point(11, 12), 13 to Point(14, 15))
    val simpleMap: Map<Int, String> = mapOf(1 to "one", 2 to "two")
    val simpleHashMap: HashMap<Int, String> = hashMapOf(3 to "three", 4 to "four")
    val simpleMutableMap: MutableMap<Int, String> = mutableMapOf(7 to "seven", 8 to "eight")
    val emptyMap: Map<Int, Point> = emptyMap()
    val nestedMap: Map<Int, List<Point>> = linkedMapOf(1 to listOf(Point(4, 5), Point(6, 7)), 2 to emptyList())
    return
}

data class Point(val x: Int, val y: Int)
