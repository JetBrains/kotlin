// KIND: STANDALONE
// MODULE: H3Generics
// FILE: H3Generics.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC

open class Box<T>(open var value: T) {
    open fun current(): T = value

    companion object {
        fun tag(): String = "BoxCompanion"
    }

    class Meta(val label: String)
}

class IntBox(v: Int) : Box<Int>(v)

open class Animal(val name: String)
open class Dog(name: String, val breed: String) : Animal(name)

fun makeDogInAnimalBox(): Box<Animal> = Box(Dog("Bolt", "Shepherd"))
fun <T> idValue(x: T): T = x
fun takeAnyValue(x: Any): String = x.toString()
fun takeNullableAnyValue(x: Any?): String = x?.toString() ?: "null"
fun anyChar(): Any = 'Z'

enum class Mode { FAST, SAFE }
fun echoMode(m: Mode): Mode = m

// 1. PAT Generic Interfaces & Nominal Wrapper
interface Repository<T> {
    fun get(): T
    fun save(item: T): Boolean
}

private class HiddenIntRepo(private var state: Int) : Repository<Int> {
    override fun get(): Int = state
    override fun save(item: Int): Boolean {
        state = item
        return true
    }
}

fun makeHiddenIntRepo(initial: Int): Repository<Int> = HiddenIntRepo(initial)
fun wrapRepoInBox(repo: Repository<Int>): Box<Repository<Int>> = Box(repo)
fun <T> syncRepo(repo: Repository<T>, next: T): T {
    repo.save(next)
    return repo.get()
}

class IntRepo(private var v: Int) : Repository<Int> {
    override fun get(): Int = v
    override fun save(item: Int): Boolean {
        v = item
        return true
    }
}

class BoxRepo<T>(private var v: T) : Repository<T> {
    override fun get(): T = v
    override fun save(item: T): Boolean {
        v = item
        return true
    }
}

interface StringRepository : Repository<String> {
    fun upper(): String
}

class MemoryStringRepo(private var s: String) : StringRepository {
    override fun get(): String = s
    override fun save(item: String): Boolean {
        s = item
        return true
    }
    override fun upper(): String = s.uppercase()
}

// Sibling name collision with AnyStream -> _AnyStream<T>
interface Stream<T> {
    fun nextItem(): T
}
class AnyStream(val label: String)
private class HiddenStream<T>(private val v: T) : Stream<T> {
    override fun nextItem(): T = v
}
fun <T> singleStream(v: T): Stream<T> = HiddenStream(v)

// 2. Declaration-site Variance & Use-site Input Projection Lifting
class OutBox<out T>(val value: T)

class InSink<in T>(val prefix: String) {
    private var lastRendered: String = ""
    fun accept(item: T): String {
        lastRendered = "$prefix:$item"
        return lastRendered
    }
}

interface Source<out T> {
    fun fetch(): T
}

class SimpleSource<out T>(private val v: T) : Source<T> {
    override fun fetch(): T = v
}

interface ConsumerSink<in T> {
    fun push(value: T): String
}

class AnimalNameSink : ConsumerSink<Animal> {
    override fun push(value: Animal): String = "animal:${value.name}"
}

fun describeStarBox(b: Box<*>): String = "star:${b.value}"
fun readOutAnimalName(b: Box<out Animal>): String = b.value.name

// 3. Multi-Bounds, Protocol F-Bounds, & Class F-Bound Relaxation
interface Named {
    val title: String
}

class TrainedDog(
    name: String,
    override val title: String,
    private var tricks: Int,
) : Dog(name, "Collie"), Named, Repository<Int> {
    override fun get(): Int = tricks
    override fun save(item: Int): Boolean {
        tricks = item
        return true
    }
}

fun <T> describeMultiBound(x: T): String where T : Animal, T : Named, T : Repository<Int> =
    "${x.name}:${x.title}:${x.get()}"

class DualEndpoint<T>(val endpoint: T) where T : Source<String>, T : ConsumerSink<String> {
    fun ping(msg: String): String = "${endpoint.push(msg)}->${endpoint.fetch()}"
}

class EchoEndpoint(private var buf: String) : Source<String>, ConsumerSink<String> {
    override fun fetch(): String = buf
    override fun push(value: String): String {
        buf = value
        return "ack:$value"
    }
}

interface MyComparable<T> {
    fun compareToOther(other: T): Int
}

class Score(val points: Int) : MyComparable<Score> {
    override fun compareToOther(other: Score): Int = points - other.points
}

fun <T : MyComparable<T>> maxOfTwo(a: T, b: T): T =
    if (a.compareToOther(b) >= 0) a else b

fun <T : MyComparable<T>> T.isAtLeast(other: T): Boolean =
    this.compareToOther(other) >= 0

open class SelfRef<T : SelfRef<T>>(val tag: String)
class ConcreteSelfRef(tag: String) : SelfRef<ConcreteSelfRef>(tag)

// 4. @frozen Generic Sealed Class & Sealed Interface
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    class Success<T>(val data: T) : UiState<T>()
    class Failed(val reason: String) : UiState<Nothing>()
}

fun <T> makeSuccessState(data: T): UiState<T> = UiState.Success(data)
fun <T> makeLoadingState(): UiState<T> = UiState.Loading

sealed interface ApiResult<out T> {
    data object Empty : ApiResult<Nothing>
    class Ok<T>(val payload: T) : ApiResult<T>
}

fun <T> makeOkResult(payload: T): ApiResult<T> = ApiResult.Ok(payload)
fun <T> makeEmptyResult(): ApiResult<T> = ApiResult.Empty

// 5. Collections with Hashable inference, closures, and GC StableRef probe
fun <K> singletonSet(k: K): Set<K> = setOf(k)
fun <K, V> singletonMap(k: K, v: V): Map<K, V> = mapOf(k to v)
fun <T, R> mapBox(box: Box<T>, fn: (T) -> R): Box<R> = Box(fn(box.value))

fun currentStableRefs(): Long {
    GC.collect()
    GC.collect()
    return GC.lastGCInfo?.rootSet?.stableReferences ?: -1L
}
