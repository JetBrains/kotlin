// KIND: STANDALONE_LLDB
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
