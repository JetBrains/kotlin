// EXIT_CODE: 42
// OUTPUT_DATA_FILE: exitProcess.out

import kotlin.system.*

fun main() {
    print("OK")
    exitProcess(42)
    println("FAIL")
}