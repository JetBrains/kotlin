// KIND: STANDALONE
// MODULE: H3Coroutines
// FILE: H3Coroutines.kt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class AsyncBox<T>(val value: T)

interface AsyncRepo<T> {
    fun current(): T
}

private class HiddenAsyncRepo<T>(private val v: T) : AsyncRepo<T> {
    override fun current(): T = v
}

fun <T> makeAsyncRepo(v: T): AsyncRepo<T> = HiddenAsyncRepo(v)

suspend fun <T> asyncEcho(x: T): T = x

suspend fun <T, R> asyncMap(x: T, transform: suspend (T) -> R): R = transform(x)

fun <T> flowOfBoxes(a: T, b: T): Flow<AsyncBox<T>> = flowOf(AsyncBox(a), AsyncBox(b))

fun <T> flowOfRepos(a: T, b: T): Flow<AsyncRepo<T>> = flowOf(HiddenAsyncRepo(a), HiddenAsyncRepo(b))

fun nullableIntStateFlow(initial: Int?): MutableStateFlow<Int?> = MutableStateFlow(initial)
