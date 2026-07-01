// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectArrayChildrenWithCache.in
// OUTPUT_DATA_FILE: canInspectArrayChildrenWithCache.out



fun main(args: Array<String>) {
    val xs = intArrayOf(3, 5, 8)
    return
}

data class Point(val x: Int, val y: Int)
