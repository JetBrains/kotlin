// EXPECTED_ERROR_K2: (kotlin:5:1) cannot find symbol
// CORRECT_ERROR_TYPES
// WITH_STDLIB

class MappedList<out T, R>(val list: List<T>, private val function: (T) -> R) : AbstractList<R>(), List<R> {
    override fun get(index: Int) = function(list[index])
    override val size get() = list.size
}
