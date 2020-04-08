// "Replace with 'runSuspend(block)'" "true"

fun runSuspend(block: suspend () -> Unit) {}
@Deprecated("Use new function", ReplaceWith("runSuspend(block)"))
fun runSuspendOld(block: suspend () -> Unit) = runSuspend(block)

fun println() {}

fun usage() {
    <caret>runSuspendOld { println() }
}