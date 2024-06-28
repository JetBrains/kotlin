import kotlin.coroutines.*

abstract class Generator<T> {
    private var generatorContinuation: Continuation<Unit>? = null
    private var callerContinuation: Continuation<T>? = null

    fun resetGenerator() {
        this::initGenerator.startCoroutine(object: Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                result.getOrThrow()
            }
        })
    }

    suspend fun yieldValue(x: T) {
        suspendCoroutine { continuation ->
            generatorContinuation = continuation
            callerContinuation?.resume(x)
        }
    }

    private suspend fun initGenerator() {
        suspendCoroutine { continuation ->
            generatorContinuation = continuation
        }

        generatorBody()

        generatorContinuation = null
    }

    protected abstract suspend fun generatorBody()

    fun hasNext(): Boolean {
        return generatorContinuation != null
    }

    suspend fun nextValue(): T {
        return suspendCoroutine { continuation ->
            callerContinuation = continuation
            generatorContinuation?.resume(Unit)
        }
    }
}

class ClosedRangeGenerator(
    private val rangeStart: Int,
    private val rangeEnd: Int,
    private val step: Int
) : Generator<Int>() {
    override suspend fun generatorBody() {
        for (i in IntProgression.fromClosedRange(rangeStart, rangeEnd, step)) {
            yieldValue(i)
        }
    }
}
