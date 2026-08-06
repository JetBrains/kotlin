// KIND: STANDALONE_LLDB

fun main(args: Array<String>) {
    val a = "string literal"
    val b = buildString {
        append("dynamic ")
        append("string")
    }
    return
}
