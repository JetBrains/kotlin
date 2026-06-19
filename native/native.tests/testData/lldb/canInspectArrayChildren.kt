// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectArrayChildren.in
// OUTPUT_DATA_FILE: canInspectArrayChildren.out
fun main(args: Array<String>) {
    val xs = intArrayOf(3, 5, 8)
    return
}

data class Point(val x: Int, val y: Int)
