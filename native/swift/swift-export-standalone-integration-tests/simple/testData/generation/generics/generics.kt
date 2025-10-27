// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

fun <T> foo(param1: T, param2: T?) {}

// A producer interface (covariant)
interface Producer<out T> {
    fun produce(): T
}

// A consumer interface (contravariant)
interface Consumer<in T> {
    fun consume(item: T)
}

// An example producer class
open class StringProducer : Producer<String> {
    override fun produce(): String = "Hello"
}

// An example consumer class
class AnyConsumer : Consumer<Any> {
    override fun consume(item: Any) {
        println("Consumed: $item")
    }
}

class Pair<K, V>(val first: K, val second: V)

fun <K, V> createMap(pairs: List<Pair<K, V>>): Map<K, V> {
    return pairs.associate { it.first to it.second }
}

interface Processor<T, R> {
    fun process(input: T): R
}

class IdentityProcessor<T> : Processor<T, T> {
    override fun process(input: T): T = input
}

fun <T> List<T>.customFilter(predicate: (T) -> Boolean): List<T> {
    return this.filter(predicate)
}

interface ConsumerProducer<T> : Consumer<T>, Producer<T>

class CPImpl: StringProducer(), ConsumerProducer<String> {
    override fun consume(item: String) {
        println("Consumed: $item")
    }
}

interface A <T> {
    val foo: T
}

interface B <T> {
    val foo: T
}

class Demo: A<Int>, B<Int?> {
    override val foo = 5
}

abstract class Box<T>(val t: T)
class DefaultBox<T>(t: T): Box<T>(t)
class TripleBox: Box<Box<Box<Int>>>(DefaultBox(DefaultBox(5)))

class GenericWithComparableUpperBound<T: Comparable<T>>(val t: T)

// Minimal repro for KT-79105: having Array<T> in the API surface must not crash Swift export.

//data class ArrayBox(val ints: Array<Int>)
//

class ArrayBox() {
    val ints: Array<Int> = emptyArray()
}

class Holder<T>(val xs: Array<T>) {
    fun headOrNull(): T? = if (xs.isNotEmpty()) xs[0] else null
}

//fun makeArrayBox(): ArrayBox = ArrayBox(arrayOf(1, 2, 3))

// Make sure arrays appear in both public signatures and bodies.
fun takeAndReturn(a: Array<String>): Array<String> = a

// KT-79227 Swift Export: Fix First Release Issues
// Generic type erasure produces conflicts at call site
//fun <T : Map<String, Int>> conflictTopLevel(a: T, b: Map<Int, String>): Int {
//    return a.size + b.size
//}
//
//class ConflictInMember<T : List<String>>(private val first: T) {
//    fun conflictMember(second: List<Int>): Int {
//        return first.size + second.size
//    }
//}
//
//interface ConflictInInterface<T : Set<String>> {
//    fun conflictInSignature(x: T, y: Set<Int>)
//}

//fun <T : List<Pair<String, Int>>> conflictNested(a: T, b: List<Pair<Int, String>>) {
//    // no-op
//}

//fun <T : Map<String, Int>> conflictReturn(a: Map<Int, String>): T? = null
