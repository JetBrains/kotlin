// TEST_RUNNER: LLDB
// FREE_COMPILER_ARGS: -g -Xg-generate-debug-trampoline=enable
// LLDB_SESSION: kt33364.pat
// FILE: kt33364.kt
fun main() {
    val param = 3

    //breakpoint here (line: 4, breakpoint is set to 5th line)
    when(param) {
        1 -> print("A")
        2 -> print("B")
        else -> print("C")
    }

    // breakpoint here (line: 11, breakpoint is set to 12th line)
    when {
        param == 1 -> print("A")
        param == 2 -> print("B")
        else -> print("C")
    }
}
