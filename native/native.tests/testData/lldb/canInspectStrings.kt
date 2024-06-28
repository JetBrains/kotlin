// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
fun main(args: Array<String>) {
    val a = "string literal"
    val b = buildString {
        append("dynamic ")
        append("string")
    }
    return
}
