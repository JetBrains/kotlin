// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: H3AsyncPatAndFlows
// SWIFT_EXPORT_CONFIG: enableGenericsSupport=true
// FILE: H3AsyncPatAndFlows.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

open class Box<T>(open var value: T)

open class Animal(val name: String)
open class Dog(name: String, val breed: String) : Animal(name)

// --- 1. Cross-Language Suspend Generic PAT Interfaces ---

class AsyncRepoException(val errorCode: Int, message: String) : Exception(message)

interface AsyncRepo<T> {
    suspend fun fetch(): T
    suspend fun put(item: T): Boolean

    @Throws(Throwable::class)
    suspend fun fetchOrFail(fail: Boolean): T
}

private class HiddenAsyncRepo<T>(private var item: T) : AsyncRepo<T> {
    override suspend fun fetch(): T {
        delay(1)
        return item
    }

    override suspend fun put(item: T): Boolean {
        delay(1)
        this.item = item
        return true
    }

    override suspend fun fetchOrFail(fail: Boolean): T {
        delay(1)
        if (fail) throw AsyncRepoException(500, "hidden-repo-failure")
        return item
    }
}

class KotlinAsyncRepo<T>(private var item: T, private val failCode: Int) : AsyncRepo<T> {
    override suspend fun fetch(): T {
        delay(1)
        return item
    }

    override suspend fun put(item: T): Boolean {
        delay(1)
        this.item = item
        return true
    }

    override suspend fun fetchOrFail(fail: Boolean): T {
        delay(1)
        if (fail) throw AsyncRepoException(failCode, "kotlin-repo-fail-$failCode")
        return item
    }
}

fun <T> makeHiddenAsyncRepo(initial: T): AsyncRepo<T> = HiddenAsyncRepo(initial)

suspend fun <T> roundTripViaAsyncRepo(repo: AsyncRepo<T>, next: T): Pair<T, T> {
    val before = repo.fetch()
    delay(1)
    val accepted = repo.put(next)
    if (!accepted) throw AsyncRepoException(409, "put rejected")
    val after = repo.fetch()
    return Pair(before, after)
}

suspend fun <T> invokeFetchOrFailFromKotlin(repo: AsyncRepo<T>, fail: Boolean): T {
    delay(1)
    return repo.fetchOrFail(fail)
}

suspend fun <T> catchFetchOrFailInKotlin(repo: AsyncRepo<T>, fail: Boolean): String {
    return try {
        val v = repo.fetchOrFail(fail)
        "ok:$v"
    } catch (t: Throwable) {
        "caught:${t.message ?: t::class.simpleName}"
    }
}

// --- 2. KMP ViewModel Flow / StateFlow / SharedFlow & Suspend Higher-Order Generics ---

class User(val id: Int, val handle: String)

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    class Ready<T>(val data: T) : UiState<T>()
    class Error(val reason: String) : UiState<Nothing>()
}

class KmpViewModel<T>(initial: UiState<T>) {
    private val _state: MutableStateFlow<UiState<T>> = MutableStateFlow(initial)
    val state: StateFlow<UiState<T>> get() = _state.asStateFlow()
    val mutableState: MutableStateFlow<UiState<T>> get() = _state

    suspend fun transitionToReady(data: T) {
        delay(1)
        _state.emit(UiState.Ready(data))
    }

    fun transitionToError(reason: String) {
        _state.value = UiState.Error(reason)
    }
}

fun makeUserViewModel(): KmpViewModel<User> = KmpViewModel(UiState.Loading)

fun makeNullableIntStateFlow(initial: Int?): MutableStateFlow<Int?> =
    MutableStateFlow(initial)

class BoxEventBus<T>(replay: Int) {
    private val _events = MutableSharedFlow<Box<T>>(replay = replay, extraBufferCapacity = 8)
    val events: SharedFlow<Box<T>> get() = _events.asSharedFlow()
    val mutableEvents: MutableSharedFlow<Box<T>> get() = _events

    suspend fun publishFromKotlin(item: T) {
        delay(1)
        _events.emit(Box(item))
    }
}

fun streamOfIntRepos(swiftRepo: AsyncRepo<Int>): Flow<AsyncRepo<Int>> = flow {
    emit(HiddenAsyncRepo(10))
    delay(1)
    emit(KotlinAsyncRepo(20, 502))
    delay(1)
    emit(swiftRepo)
}

suspend fun <T, R> asyncMapBox(b: Box<T>, transform: suspend (T) -> R): Box<R> {
    delay(1)
    return Box(transform(b.value))
}

suspend fun <A, B, C> asyncChainBoxes(
    b: Box<A>,
    first: suspend (A) -> B,
    second: suspend (B) -> C,
): Box<C> {
    val mid = first(b.value)
    delay(1)
    return Box(second(mid))
}

fun currentStableRefs(): Long {
    GC.collect()
    GC.collect()
    return GC.lastGCInfo?.rootSet?.stableReferences ?: -1L
}
