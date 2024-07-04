// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// INPUT_DATA_FILE: canStepThroughCode.in
// OUTPUT_DATA_FILE: canStepThroughCode.out
fun main(args: Array<String>) {
    var x = 1
    var y = 2
    var z = x + y
    println(z)
}
