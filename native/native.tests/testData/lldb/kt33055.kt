// TEST_RUNNER: LLDB
// FREE_COMPILER_ARGS: -g -Xg-generate-debug-trampoline=enable
// LLDB_SESSION: kt33055.pat
// FILE: kt33055.kt
fun question(subject: String, names: Array<String> = emptyArray()): String {
    return buildString { // breakpoint here
        append("$subject?") // actual stop
        for (name in names)
            append(" $name?")
    }
}

fun main(args: Array<String>) {
    println(question("Subject", args))
}
