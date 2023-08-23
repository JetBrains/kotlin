// KIND: STANDALONE_LLDB
// LLDB_TRACE: canInspectStrings.txt
fun main(args: Array<String>) {
    val a = "string literal"
    val b = buildString {
        append("dynamic ")
        append("string")
    }
    return
}
