// KIND: STANDALONE_LLDB


fun test(): Nothing = error("Should not return")

fun main(args: Array<String>) {
    test()
    return
}
