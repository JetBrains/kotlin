// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: DeepGenerics
// SWIFT_EXPORT_CONFIG: enableGenericsSupport=true
// FILE: DeepGenerics.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC

// --- 1. Core Generic Carriers, Value Types, Enums & Runtime Tagging ---

open class Box<T>(open var value: T) {
    open fun snapshot(): T = value
}

open class PairBox<A, B>(open var first: A, open var second: B) {
    fun swapped(): PairBox<B, A> = PairBox(second, first)

    companion object {
        fun companionTag(): String = "PairBoxCompanion"
        fun <X, Y> of(x: X, y: Y): PairBox<X, Y> = PairBox(x, y)
    }

    class Stamp(val label: String, val version: Int) {
        fun describe(): String = "$label@$version"
    }
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

fun <T> identity(x: T): T = x

fun <A, B> swapPairBox(p: PairBox<A, B>): PairBox<B, A> = PairBox(p.second, p.first)

fun <T> echoList(items: List<T>): List<T> = items

fun <T> reverseList(items: List<T>): List<T> = items.asReversed()

fun <T> echoSet(items: Set<T>): Set<T> = items

fun <K, V> echoMap(map: Map<K, V>): Map<K, V> = map

fun <K, V> invertMap(map: Map<K, V>): Map<V, K> =
    map.entries.associate { (k, v) -> v to k }

fun <T> echoNullable(x: T?): T? = x

fun <T> kotlinRuntimeTag(x: T): String = when (x) {
    null -> "null"
    is Byte -> "Byte:$x"
    is Short -> "Short:$x"
    is Int -> "Int:$x"
    is Long -> "Long:$x"
    is UByte -> "UByte:$x"
    is UShort -> "UShort:$x"
    is UInt -> "UInt:$x"
    is ULong -> "ULong:$x"
    is Float -> "Float:$x"
    is Double -> "Double:$x"
    is Boolean -> "Boolean:$x"
    is Char -> "Char:${x.code}:$x"
    is Unit -> "Unit"
    is Priority -> "Priority:${x.name}"
    is String -> "String:$x"
    else -> "Other:${x::class.simpleName}:$x"
}

fun makeBoxedKotlinAny(seed: Int): Box<Any> = Box("any-$seed")

fun makeKotlinCharBox(c: Char): Box<Char> = Box(c)

fun describeKotlinChar(c: Char): String = "Char:${c.code}:$c"

fun makeKotlinBmpChars(): List<Char> = listOf('A', 'Ω', 'λ')

fun joinKotlinChars(chars: List<Char>): String = chars.joinToString("")

fun echoKotlinCharSet(chars: Set<Char>): Set<Char> = chars

fun makeKotlinCharMap(): Map<String, Char> = mapOf("ascii" to 'A', "omega" to 'Ω', "lambda" to 'λ')

fun invertKotlinCharMap(map: Map<String, Char>): Map<Char, String> =
    map.entries.associate { (k, v) -> v to k }

fun joinVarargChars(vararg chars: Char): String = chars.concatToString()

fun makeBoxedCharAsAny(c: Char): Box<Any> = Box(c)

fun makeBoxedUnitAsAny(): Box<Any> = Box(Unit)

fun inspectBoxedAny(b: Box<Any>): String = kotlinRuntimeTag(b.value)

// --- 2. Deep Nesting, Nullability Edge Cases & PATs ---

class NullableHolder<T>(var item: T?) {
    fun isItemNull(): Boolean = item == null
    fun replace(next: T?): T? {
        val prev = item
        item = next
        return prev
    }
}

open class Animal(val name: String) {
    override fun toString(): String = "Animal($name)"
}

open class Dog(name: String, val breed: String) : Animal(name) {
    override fun toString(): String = "Dog($name,$breed)"
}

open class Puppy(name: String, breed: String, val ageWeeks: Int) : Dog(name, breed) {
    override fun toString(): String = "Puppy($name,$breed,${ageWeeks}w)"
}

class ShelterSlot<T : Animal?>(var occupant: T) {
    fun occupantLabel(): String = occupant?.name ?: "vacant"
}

fun <T : Animal?> describeOccupant(occupant: T): String =
    occupant?.let { "${it::class.simpleName}:${it.name}" } ?: "none"

interface Repo<T> {
    fun load(): T
    fun store(item: T): Boolean
}

private class HiddenRepo<T>(private var current: T) : Repo<T> {
    override fun load(): T = current
    override fun store(item: T): Boolean {
        current = item
        return true
    }
}

class MemRepo<T>(private var current: T) : Repo<T> {
    override fun load(): T = current
    override fun store(item: T): Boolean {
        current = item
        return true
    }
}

fun <T> makeHiddenRepo(initial: T): Repo<T> = HiddenRepo(initial)

fun <T> wrapRepoInBox(repo: Repo<T>): Box<Repo<T>> = Box(repo)

fun <T> updateBoxedRepo(boxed: Box<Repo<T>>, next: T): T {
    boxed.value.store(next)
    return boxed.value.load()
}

fun makeNestedIntBox(v: Int): Box<Box<Int>> = Box(Box(v))

fun makeTripleNestedBox(s: String): Box<Box<Box<String>>> = Box(Box(Box(s)))

fun echoNestedNullableStrings(b: Box<List<String?>>): Box<List<String?>> =
    Box(b.value.map { it?.uppercase() })

fun sumNonNullBoxes(boxes: List<Box<Int?>>): Int =
    boxes.sumOf { it.value ?: 0 }

fun lookupDogBreed(directory: Map<String, Box<Dog>>, key: String): String? =
    directory[key]?.value?.breed

// --- 3. Higher-Order Generic Functions, Curried Closures, @Throws & Varargs ---

fun <A, B, C> combineValues(a: A, b: B, combine: (A, B) -> C): C = combine(a, b)

fun <A, B> makePairBuilder(): (A, B) -> PairBox<A, B> = { a, b -> PairBox(a, b) }

fun <T, R> mapOrDefault(value: T, fallback: R, transform: ((T) -> R)?): R =
    if (transform != null) transform(value) else fallback

fun <T> makeOptionalBoxer(enabled: Boolean): ((T) -> Box<T>)? =
    if (enabled) ({ v: T -> Box(v) }) else null

fun <T, U> makeCurriedPairBox(): (T) -> ((U) -> PairBox<T, U>) =
    { t -> { u -> PairBox(t, u) } }

fun <T, U> applyCurriedPairBox(t: T, u: U, curried: (T) -> ((U) -> PairBox<T, U>)): PairBox<T, U> =
    curried(t)(u)

fun <T, U> makeCurriedStdlibPair(): (T) -> ((U) -> Pair<T, U>) =
    { t -> { u -> Pair(t, u) } }

fun <T, U> applyCurriedStdlibPair(t: T, u: U, curried: (T) -> ((U) -> Pair<T, U>)): Pair<T, U> =
    curried(t)(u)

fun <T, U> applyHigherOrder(seed: T, mapper: (T) -> U, runner: ((T) -> U) -> U): U =
    runner(mapper)

fun <T, U> makeHigherOrderInvoker(seed: T): ((T) -> U) -> U =
    { fn -> fn(seed) }

fun <T, U> makeOptionalCurried(active: Boolean): (T) -> (((U) -> PairBox<T, U>)?) =
    { t -> if (active) ({ u: U -> PairBox(t, u) }) else null }

fun <T> makeOptionalRepoFactory(active: Boolean): (T) -> Repo<T>? =
    { t -> if (active) HiddenRepo(t) else null }

fun <T, U> applyOptionalCurried(
    t: T,
    u: U,
    fallback: PairBox<T, U>,
    curried: (T) -> (((U) -> PairBox<T, U>)?),
): PairBox<T, U> {
    val step = curried(t)
    return if (step != null) step(u) else fallback
}

fun <T, Acc> foldItems(items: List<T>, initial: Acc, step: (Acc, T) -> Acc): Acc {
    var acc = initial
    for (item in items) {
        acc = step(acc, item)
    }
    return acc
}

class GenericValidationException(val code: Int, message: String) : Exception(message)

@Throws(GenericValidationException::class)
fun <T> requireValidItem(item: T, errorCode: Int, predicate: (T) -> Boolean): Box<T> {
    if (!predicate(item)) {
        throw GenericValidationException(errorCode, "Rejected: $item")
    }
    return Box(item)
}

fun <T> packVarargs(vararg items: T): List<T> = items.toList()

fun <T> firstAndLastVararg(vararg items: T): PairBox<T, T>? =
    if (items.isEmpty()) null else PairBox(items.first(), items.last())

// --- 4. Multi-Level Sealed Hierarchies, 3-Level Variance Chains, Bounds & Typealiases ---

sealed interface Outcome<out T> {
    class Value<T>(val data: T) : Outcome<T>

    sealed class Failure(val category: String) : Outcome<Nothing> {
        data object Timeout : Failure("timeout")
        class HttpError(val code: Int, val reason: String) : Failure("http")
    }
}

fun <T> outcomeValue(data: T): Outcome<T> = Outcome.Value(data)
fun <T> outcomeTimeout(): Outcome<T> = Outcome.Failure.Timeout
fun <T> outcomeHttpError(code: Int, reason: String): Outcome<T> = Outcome.Failure.HttpError(code, reason)

fun inspectAnimalOutcome(outcome: Outcome<Animal>): String = when (outcome) {
    is Outcome.Value -> "ok:${outcome.data.name}"
    is Outcome.Failure.Timeout -> "err:timeout"
    is Outcome.Failure.HttpError -> "err:http:${outcome.code}:${outcome.reason}"
}

class OutBox<out T>(val item: T)

class InSink<in T>(private val label: String) {
    fun consume(value: T): String = "$label:$value"
}

interface Source<out T> {
    fun next(): T
}

class FixedSource<out T>(private val item: T) : Source<T> {
    override fun next(): T = item
}

interface Sink<in T> {
    fun accept(item: T): String
}

class AnimalSink(private val tag: String) : Sink<Animal> {
    override fun accept(item: Animal): String = "$tag:${item.name}"
}

fun readAnimalFromOutBox(box: OutBox<Animal>): String = box.item.name
fun readAnimalFromSource(src: Source<Animal>): String = src.next().name
fun feedPuppyToSink(sink: Sink<Puppy>, puppy: Puppy): String = sink.accept(puppy)

interface Named {
    val badge: String
}

interface MyComparable<T> {
    fun compareToOther(other: T): Int
}

class ChampionPuppy(
    name: String,
    breed: String,
    ageWeeks: Int,
    override val badge: String,
    val score: Int,
) : Puppy(name, breed, ageWeeks), Named, MyComparable<ChampionPuppy> {
    override fun compareToOther(other: ChampionPuppy): Int = score - other.score
}

fun <T> crownChampion(a: T, b: T): T where T : Animal, T : Named, T : MyComparable<T> =
    if (a.compareToOther(b) >= 0) a else b

class Podium<T>(val first: T, val second: T) where T : Animal, T : Named, T : MyComparable<T> {
    fun winnerSummary(): String {
        val w = if (first.compareToOther(second) >= 0) first else second
        return "${w.name}#${w.badge}"
    }
}

typealias BoxedList<T> = Box<List<T>>
typealias KeyedBoxes<K, V> = Map<K, Box<V>>
typealias GenericMapper<T, R> = (T) -> R

fun <T> makeBoxedList(items: List<T>): BoxedList<T> = Box(items)
fun <K, V> unwrapKeyedBox(map: KeyedBoxes<K, V>, key: K): V? = map[key]?.value
fun <T, R> runGenericMapper(item: T, mapper: GenericMapper<T, R>): R = mapper(item)

fun currentStableRefs(): Long {
    GC.collect()
    GC.collect()
    return GC.lastGCInfo?.rootSet?.stableReferences ?: -1L
}

// FILE: SubPkg.kt
package sub.pkg

import Box

class SubEnvelope<T>(var payload: T, val code: Int) {
    fun repackage(next: T): SubEnvelope<T> = SubEnvelope(next, code + 1)
}

interface SubCodec<T> {
    fun encode(value: T): String
    fun decode(raw: String): T
}

class SubStringCodec(private val prefix: String) : SubCodec<String> {
    override fun encode(value: String): String = "$prefix:$value"
    override fun decode(raw: String): String = raw.removePrefix("$prefix:")
}

fun <T> makeSubEnvelope(payload: T, code: Int): SubEnvelope<T> = SubEnvelope(payload, code)

fun <T> roundTripSubCodec(codec: SubCodec<T>, value: T): T =
    codec.decode(codec.encode(value))

fun <T> boxSubEnvelope(env: SubEnvelope<T>): Box<SubEnvelope<T>> = Box(env)
