// IS_APPLICABLE: false
// COMPILER_ARGUMENTS: -XXLanguage:-SuspendConversion

fun coroutine(block: suspend () -> Unit) {}

suspend fun testAction(obj: Any, action: suspend (Any) -> Unit) {
    action(action)
}

fun println(message: Any?) {}

fun main() = coroutine {
    testAction("OK") {<caret>
        println(it)
    }
}