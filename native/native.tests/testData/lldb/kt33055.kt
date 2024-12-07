// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xg-generate-debug-trampoline=enable
// FIR_IDENTICAL
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
