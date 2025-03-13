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
class StringProducer : Producer<String> {
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