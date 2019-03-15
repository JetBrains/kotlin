// CORRECT_ERROR_TYPES
// WITH_RUNTIME

class MappedList<out T, R>(val list: List<T>, private val function: (T) -> R) : AbstractList<R>(), List<R> {
    override fun get(index: Int) = function(list[index])
    override val size get() = list.size
}