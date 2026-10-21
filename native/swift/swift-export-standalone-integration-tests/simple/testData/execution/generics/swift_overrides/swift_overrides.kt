// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Describe.kt
// Shared Kotlin-side probes: tell Swift what Kotlin actually received through Any / T.

fun describeAny(x: Any?): String = when (x) {
    is String -> "String:$x"
    is Int -> "Int:$x"
    else -> "Other"
}

// FILE: Variance.kt
// Declaration-site `in` variance: Swift implements a contravariant Kotlin interface.

interface Consumer<in T> { fun consume(x: T): String }
@Suppress("UNCHECKED_CAST")
fun <T> consumeKotlinValues(c: Consumer<T>): String =
    listOf<Any?>("k", 1, listOf("l")).joinToString(",") { c.consume(it as T) }

// FILE: ReverseDirection.kt
// Swift implements / overrides a Kotlin declaration and hands values back through T / Any.

interface Producer<T> {
    fun produce(): T
}

interface AnyProducer {
    fun produceAny(): Any
}

open class OpenAnyProducer {
    open fun produceAny(): Any = "kotlin-default"
}

fun <T> consumeProducer(p: Producer<T>): String = describeAny(p.produce())
fun consumeAnyProducer(p: AnyProducer): String = describeAny(p.produceAny())
fun consumeOpenProducer(p: OpenAnyProducer): String = describeAny(p.produceAny())
fun isString(p: AnyProducer): Boolean = p.produceAny() is String

// FILE: SwiftOverridesInt.kt
// Kotlin declares Any / T (erased in Swift to its upper bound, `_KotlinBridgeable`); a Swift subclass or
// implementation returns Int32 there; Kotlin then computes with the value as a real kotlin.Int.

open class NullableAnySource { open fun get(): Any? = null }
open class GenericSource<T>(private val initial: T) { open fun get(): T = initial }
open class AnySink { open fun accept(x: Any): String = "kotlin" }

fun nullable(s: NullableAnySource): Int = (s.get() as Int?)?.plus(1) ?: -1
fun <T> genericDouble(s: GenericSource<T>): Int = (s.get() as Int) * 2
fun sendIntToSwift(sink: AnySink): String = sink.accept(42)
