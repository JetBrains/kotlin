// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// FIR_IDENTICAL
// INPUT_DATA_FILE: canInspectStrings.in
// OUTPUT_DATA_FILE: canInspectStrings.out



fun main(args: Array<String>) {
    val a = "string literal"
    val b = buildString {
        append("dynamic ")
        append("string")
    }
    return
}
