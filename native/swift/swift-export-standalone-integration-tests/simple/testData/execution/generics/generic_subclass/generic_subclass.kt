// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Describe.kt
// Shared Kotlin-side probes: tell Swift what Kotlin actually received through Any / T.

fun describeAny(x: Any?): String = when (x) {
    is String -> "String:$x"
    is Int -> "Int:$x"
    is List<*> -> "List[" + x.joinToString(",") { describeAny(it) } + "]"
    else -> "Other"
}

// FILE: GenericSubclass.kt
// Swift subclasses a Kotlin class with a type parameter (T erased to its upper bound in Swift);
// T also held as properties. Both directions: Swift calls the Kotlin implementation, Kotlin calls the Swift override.

open class Holder<T>(open val item: T) {
    val items: List<T> get() = listOf(item)
    open fun get(): T = item
    open fun transform(x: T): T = x
}

fun <T> callGet(h: Holder<T>): String = describeAny(h.get())
fun readItem(h: Holder<*>): String = describeAny(h.item)
fun readItems(h: Holder<*>): String = describeAny(h.items)
@Suppress("UNCHECKED_CAST")
fun <T> callTransformWithKotlinValue(h: Holder<T>): String = describeAny(h.transform("from-kotlin" as T))

// An instance of a Swift subclass travelling through unbound T / Any comes back as the same object.
fun <T> echoT(x: T): T = x
fun echoAny(x: Any): Any = x
