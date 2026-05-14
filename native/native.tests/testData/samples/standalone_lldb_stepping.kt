// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// INPUT_DATA_FILE: standalone_lldb_stepping.in
// OUTPUT_DATA_FILE: standalone_lldb_stepping.out
import kotlin.test.*

fun main(args: Array<String>) {
    var x = 1
    var y = 2
    var z = x + y
    println(z)
}
