// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xg-generate-debug-trampoline=enable
// FIR_IDENTICAL
// INPUT_DATA_FILE: kt33364.in
// OUTPUT_DATA_FILE: kt33364.out
// FILE: kt33364.kt
fun main() {
    val param = 3

    //breakpoint here (line: 8, breakpoint is set to 9th line)
    when(param) {
        1 -> print("A")
        2 -> print("B")
        else -> print("C")
    }

    // breakpoint here (line: 15, breakpoint is set to 16th line)
    when {
        param == 1 -> print("A")
        param == 2 -> print("B")
        else -> print("C")
    }
}
