import kotlin.test.*
import kotlin.coroutines.*

// To be tested with -g.

// https://youtrack.jetbrains.com/issue/KT-49360

class Block(val block: () -> Int)

// The Flow code below is taken from kotlinx.coroutines (some unrelated details removed).

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit): Unit =
        collect(object : FlowCollector<T> {
            override suspend fun emit(value: T) = action(value)
        })

inline fun <T> unsafeFlow(crossinline block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            collector.block()
        }
    }
}

inline fun <T, R> Flow<T>.unsafeTransform(
        crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = unsafeFlow {
    collect { value ->
        return@collect transform(value)
    }
}

inline fun <T, R: Any> Flow<T>.mapNotNull(crossinline transform: suspend (value: T) -> R?): Flow<R> = unsafeTransform { value ->
    val transformed = transform(value) ?: return@unsafeTransform
    return@unsafeTransform emit(transformed)
}

fun <T> flowOf(value: T): Flow<T> = unsafeFlow {
    emit(value)
}

suspend fun <T> Flow<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    collect {
        result.add(it)
    }
    return result
}

// Close to https://youtrack.jetbrains.com/issue/KT-49360:
fun testWithFlowMapNotNull(flow: Flow<Boolean>): Flow<Block> {
    return flow.mapNotNull {
        if (it) Block({ 333 }) else null
    }
}

fun box(): String {
    lateinit var list1: List<Block>
    lateinit var list2: List<Block>

    builder {
        list1 = testWithFlowMapNotNull(flowOf(true)).toList()
        list2 = testWithFlowMapNotNull(flowOf(false)).toList()
    }

    assertEquals(1, list1.size)
    assertEquals(333, list1.single().block())

    assertEquals(0, list2.size)

    return "OK"
}

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}
