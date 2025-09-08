// KIND: STANDALONE_LLDB
// FIR_IDENTICAL

fun test(): Nothing = error("Should not return")

fun main(args: Array<String>) {
    test()
    return
}
