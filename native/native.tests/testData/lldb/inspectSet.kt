// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectSet.in
// OUTPUT_DATA_FILE: inspectSet.out



fun main(args: Array<String>) {
    val set: Set<Point> = linkedSetOf(Point(1, 2), Point(3, 4))
    val intSet: Set<Int> = linkedSetOf(1, 2, 3)
    val simpleSet: Set<Int> = setOf(7, 8)
    val simpleMutableSet: MutableSet<Int> = mutableSetOf(9, 10)
    val simpleLinkedSet: LinkedHashSet<Int> = linkedSetOf(11, 12)
    val simpleHashSet: HashSet<Int> = hashSetOf(13, 14)
    val emptySet: Set<Point> = emptySet()
    val nestedSet: Set<Set<Point>> = linkedSetOf(linkedSetOf(Point(4, 5), Point(6, 7)), emptySet())
    return
}

data class Point(val x: Int, val y: Int)
