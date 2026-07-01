// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectStringsWithCache.in
// OUTPUT_DATA_FILE: canInspectStringsWithCache.out



fun main(args: Array<String>) {
    val a = "string literal"
    val b = buildString {
        append("dynamic ")
        append("string")
    }
    return
}
