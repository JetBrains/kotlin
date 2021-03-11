package kotlinx.coroutines

interface Deferred<T> {
    suspend fun await(): T
}

interface CoroutineContext

object Dispatchers {
    object Default : CoroutineContext
}

enum class CoroutineStart {
    DEFAULT,
    LAZY,
    ATOMIC,
    UNDISPATCHED
}

interface CoroutineScope {
    val coroutineContext: CoroutineContext get() = Dispatchers.Default
}

object GlobalScope : CoroutineScope

fun <T> CoroutineScope.async(
    context: CoroutineContext = Dispatchers.Default,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    TODO()
}

suspend fun <T> withContext(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    TODO()
}

suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R = GlobalScope.block()

operator fun CoroutineContext.plus(other: CoroutineContext): CoroutineContext {
    TODO()
}

