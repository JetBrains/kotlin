package soReifiedInlineIfConditionFalse

fun main(args: Array<String>) {
    // Reified function call in if condition
    //Breakpoint!
    if (reified(11) != 11) {                                        // 1
        val a = 22
    }
}                                                                   // 2

inline fun <reified T> reified(f: T): T {
    val a = 33
    return f
}

