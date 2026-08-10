// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: nothingReturn.in
// OUTPUT_DATA_FILE: nothingReturn.out


fun test(): Nothing = error("Should not return")

fun main(args: Array<String>) {
    test()
    return
}
