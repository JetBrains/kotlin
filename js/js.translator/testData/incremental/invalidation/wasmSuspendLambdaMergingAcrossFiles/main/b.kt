import kotlin.coroutines.*

suspend fun id(x: Any): Any = x

// Structurally identical to the lambda in a.kt, so both get the same shared suspend lambda class.
fun b(x: Any): suspend () -> Any = { id(x); x }

fun runCoroutine(c: suspend () -> Any): Any? {
    var result: Any? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}
