interface SizedIterable<out T> : Iterable<T> {
    fun count(): Long
}

class View<T>(private val delegate: SizedIterable<T>) : SizedIterable<T> by delegate

abstract class EmptySized : SizedIterable<Nothing> {
    override fun count(): Long = 0
}
