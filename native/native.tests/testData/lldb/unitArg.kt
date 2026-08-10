// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: unitArg.in
// OUTPUT_DATA_FILE: unitArg.out


fun test(arg: Unit): Unit {}

fun main(args: Array<String>) {
    test(Unit)
    return
}
