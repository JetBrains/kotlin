// KIND: STANDALONE
// MODULE: CrossLangGenericInterfaces
// SWIFT_EXPORT_CONFIG: enableGenericsSupport=true
// FILE: CrossLangGenericInterfaces.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.GC

// Shared domain types
open class Animal(val name: String) {
    open fun kind(): String = "animal:$name"
    override fun toString(): String = "Animal($name)"
}

open class Dog(name: String, val breed: String) : Animal(name) {
    override fun kind(): String = "dog:$name:$breed"
    override fun toString(): String = "Dog($name,$breed)"
}

interface PetTag {
    val badge: String
}

open class TaggedDog(name: String, breed: String, override val badge: String) : Dog(name, breed), PetTag {
    override fun kind(): String = "tagged:$name:$breed:$badge"
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
}

class UserProfile(val id: Int, val handle: String) {
    override fun toString(): String = "User($id,$handle)"
}

fun echoAnyObject(x: Any): Any = x

// ============================================================================
// 1. Generic Swift class implementing a generic Kotlin PAT interface
// ============================================================================

interface Repository<T> {
    fun get(): T
    fun save(item: T): Boolean
}

class KotlinMemoryRepo<T>(private var current: T) : Repository<T> {
    override fun get(): T = current
    override fun save(item: T): Boolean {
        current = item
        return true
    }
}

fun <T> syncRepo(repo: Repository<T>, next: T): T {
    repo.save(next)
    return repo.get()
}

fun <T> readRepo(repo: Repository<T>): T = repo.get()

fun <T> echoRepo(repo: Repository<T>): Repository<T> = repo

fun mutateIntRepo(repo: Repository<Int>, delta: Int): Int {
    val next = repo.get() + delta
    repo.save(next)
    return repo.get()
}

fun mutateStringRepo(repo: Repository<String>, suffix: String): String {
    val next = repo.get() + suffix
    repo.save(next)
    return repo.get()
}

fun mutateOptionalStringRepo(repo: Repository<String?>, fallback: String): String? {
    val cur = repo.get()
    val next = if (cur == null) fallback else null
    repo.save(next)
    return repo.get()
}

fun mutateIntListRepo(repo: Repository<List<Int>>, extra: Int): List<Int> {
    val next = repo.get() + extra
    repo.save(next)
    return repo.get()
}

fun mutateDogRepo(repo: Repository<Dog>, newName: String): Dog {
    val cur = repo.get()
    val next = Dog(newName, cur.breed)
    repo.save(next)
    return repo.get()
}

fun mutatePriorityRepo(repo: Repository<Priority>): Priority {
    val next = when (repo.get()) {
        Priority.LOW -> Priority.MEDIUM
        Priority.MEDIUM -> Priority.HIGH
        Priority.HIGH -> Priority.LOW
    }
    repo.save(next)
    return repo.get()
}

class RepoRegistry<T> {
    private val items = mutableListOf<Repository<T>>()

    fun register(repo: Repository<T>) {
        items.add(repo)
    }

    fun snapshot(): List<Repository<T>> = items.toList()

    fun at(index: Int): Repository<T> = items[index]

    fun broadcastSave(value: T): Int {
        var okCount = 0
        for (repo in items) {
            if (repo.save(value)) okCount += 1
        }
        return okCount
    }
}

fun <T> readAllFromRepoList(repos: List<Repository<T>>): List<T> = repos.map { it.get() }

fun <T> echoRepoList(repos: List<Repository<T>>): List<Repository<T>> = repos

// ============================================================================
// 2. Multi-parameter generic PAT interfaces (KeyValueStore<K, V>)
// ============================================================================

interface KeyValueStore<K, V> {
    val storeTag: String
    var size: Int
    var lastValue: V?
    fun get(key: K): V?
    fun put(key: K, value: V)
}

fun <K, V> putAndReadStore(store: KeyValueStore<K, V>, key: K, value: V): V? {
    store.put(key, value)
    return store.get(key)
}

fun <K, V> readStoreKey(store: KeyValueStore<K, V>, key: K): V? = store.get(key)

fun <K, V> describeStoreState(store: KeyValueStore<K, V>): String =
    "${store.storeTag}:${store.size}:${store.lastValue}"

fun <K, V> overwriteStoreTracking(store: KeyValueStore<K, V>, newSize: Int, newLast: V?): V? {
    store.size = newSize
    store.lastValue = newLast
    return store.lastValue
}

fun <K, V> echoKeyValueStore(store: KeyValueStore<K, V>): KeyValueStore<K, V> = store

fun exerciseConcreteUserStore(store: KeyValueStore<String, UserProfile>): String {
    store.put("u1", UserProfile(1, "ada"))
    store.put("u2", UserProfile(2, "grace"))
    val u1 = store.get("u1")?.handle ?: "missing"
    val missing = store.get("u99")?.handle ?: "none"
    return "${store.storeTag}|${store.size}|$u1|$missing|${store.lastValue?.handle}"
}

// ============================================================================
// 3. Kotlin generic interface default implementations vs Swift overrides
// ============================================================================

interface Formatter<T> {
    fun raw(x: T): T
    fun format(x: T): String = "fmt:${raw(x)}"
    val tag: String get() = "default"
}

class KotlinFormatter<T>(private val transform: (T) -> T) : Formatter<T> {
    override fun raw(x: T): T = transform(x)
}

fun <T> formatFromKotlin(formatter: Formatter<T>, item: T): String =
    "${formatter.tag}|${formatter.format(item)}"

fun <T> rawFromKotlin(formatter: Formatter<T>, item: T): T =
    formatter.raw(item)

fun formatIntFromKotlin(formatter: Formatter<Int>, item: Int): String =
    "${formatter.tag}|${formatter.format(item)}"

// ============================================================================
// 4. Error propagation (@Throws) across generic PAT reverse bridges
// ============================================================================

class ValidationKotlinException(message: String) : RuntimeException(message)

interface Validator<T> {
    @Throws(Throwable::class)
    fun validate(item: T): T
}

private class KotlinPositiveIntValidator : Validator<Int> {
    @Throws(Throwable::class)
    override fun validate(item: Int): Int {
        if (item <= 0) throw ValidationKotlinException("non-positive:$item")
        return item * 10
    }
}

private class KotlinRejectingValidator<T>(private val forbidden: T) : Validator<T> {
    @Throws(Throwable::class)
    override fun validate(item: T): T {
        if (item == forbidden) throw ValidationKotlinException("forbidden:$item")
        return item
    }
}

fun makeKotlinPositiveIntValidator(): Validator<Int> = KotlinPositiveIntValidator()

fun <T> makeKotlinRejectingValidator(forbidden: T): Validator<T> = KotlinRejectingValidator(forbidden)

fun <T> validateCatchingInKotlin(validator: Validator<T>, item: T): String = try {
    "ok:${validator.validate(item)}"
} catch (e: ValidationKotlinException) {
    "kt-exc:${e.message}"
} catch (e: Throwable) {
    "swift-err:${e.message ?: "?"}"
}

@Throws(Throwable::class)
fun <T> relayValidateThroughKotlin(validator: Validator<T>, item: T): T =
    validator.validate(item)

// ============================================================================
// 5. Sub-interface refinement & multi-PAT conformances
// ============================================================================

interface Readable<R> {
    fun read(): R
}

interface Writable<W> {
    fun write(item: W): Boolean
}

interface ReadWriteRepo<T> : Readable<T>, Writable<T> {
    fun swap(next: T): T
}

interface StringReadWriteRepo : ReadWriteRepo<String> {
    fun upper(): String
}

fun <R> pullReadable(source: Readable<R>): R = source.read()

fun <W> pushWritable(sink: Writable<W>, item: W): Boolean = sink.write(item)

fun <T> exerciseReadWriteRepo(rw: ReadWriteRepo<T>, first: T, second: T): List<T> {
    val initial = pullReadable(rw)
    pushWritable(rw, first)
    val afterWrite = rw.read()
    val swappedOld = rw.swap(second)
    val finalVal = rw.read()
    return listOf(initial, afterWrite, swappedOld, finalVal)
}

fun exerciseStringReadWriteRepo(srw: StringReadWriteRepo, first: String, second: String): String {
    val trace = exerciseReadWriteRepo(srw, first, second).joinToString(",")
    return "$trace|upper=${srw.upper()}"
}

fun bridgeSeparateReadableWritable(r: Readable<Int>, w: Writable<String>): Boolean =
    w.write("sep:${r.read()}")

fun <T> relayMultiPatConformer(dual: T): Boolean where T : Readable<Int>, T : Writable<String> =
    dual.write("multi:${dual.read()}")

interface TaggedShelter<T> where T : Animal, T : PetTag {
    fun current(): T
    fun accept(pet: T): String
    fun accept(fallbackLabel: String): String = "fallback:$fallbackLabel"
}

fun <T> exerciseTaggedShelter(shelter: TaggedShelter<T>, next: T): String where T : Animal, T : PetTag {
    val initialBadge = shelter.current().badge
    val accepted = shelter.accept(next)
    val fallback = shelter.accept("guest")
    return "$initialBadge|$accepted|$fallback|${shelter.current().kind()}"
}

// ============================================================================
// 6. Swift concrete & generic subclasses of an open generic Kotlin class
//    implementing a generic PAT
// ============================================================================

open class BaseRepo<T>(open var state: T) : Repository<T> {
    var baseSaveCount: Int = 0

    override fun get(): T = state

    override fun save(item: T): Boolean {
        state = item
        baseSaveCount += 1
        return true
    }

    open fun summary(): String = "base($state)#$baseSaveCount"
}

fun <T> mutateViaBaseRepo(repo: BaseRepo<T>, next: T): String {
    repo.save(next)
    return "${repo.get()}|${repo.summary()}"
}

fun <T> assignBaseRepoStateInKotlin(repo: BaseRepo<T>, next: T): T {
    repo.state = next
    return repo.state
}

fun <T> echoBaseRepo(repo: BaseRepo<T>): BaseRepo<T> = repo

// ============================================================================
// 7. Generic methods declared inside a generic PAT interface + GC leak probe
// ============================================================================

interface Transformer<T> {
    val currentValue: T
    fun <R> mapCurrent(fn: (T) -> R): R
}

class KotlinTransformer<T>(override var currentValue: T) : Transformer<T> {
    override fun <R> mapCurrent(fn: (T) -> R): R = fn(currentValue)
}

fun <T> makeHiddenTransformer(initial: T): Transformer<T> = KotlinTransformer(initial)

fun <T, R> runMapCurrentInKotlin(transformer: Transformer<T>, fn: (T) -> R): R =
    transformer.mapCurrent(fn)

fun mapIntTransformerToLabelInKotlin(transformer: Transformer<Int>): String =
    transformer.mapCurrent { x -> "mapped:${x * 5}" }

fun <T> mapTransformerToDogInKotlin(transformer: Transformer<T>, breed: String): Dog =
    transformer.mapCurrent { x -> Dog("dog-$x", breed) }

class IntCollector {
    private val seen = mutableListOf<Int>()
    fun add(v: Int) {
        seen.add(v)
    }
    fun snapshot(): List<Int> = seen.toList()
}

fun collectViaUnitMapCurrentInKotlin(transformer: Transformer<Int>, collector: IntCollector) {
    transformer.mapCurrent { x ->
        collector.add(x * 3)
    }
}

fun currentStableRefs(): Long {
    GC.collect()
    GC.collect()
    return GC.lastGCInfo?.rootSet?.stableReferences ?: -1L
}
