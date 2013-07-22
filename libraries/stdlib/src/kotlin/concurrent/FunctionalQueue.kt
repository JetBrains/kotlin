package kotlin.concurrent
import java.util.concurrent.Executor

class FunctionalQueue<T>(
        val head : T? = null,
        val middle : FunctionalQueue<Pair<T, T>>? = null,
        val tail : T? = null) {
    private fun size() : Int {
        var result : Int = 0
        if (head != null) result++
        if (tail != null) result++
        if (middle != null) result += middle.size * 2
        return result
    }
    val size : Int
        get() = size()
    val empty : Boolean
        get() = head == null && tail == null && (middle == null || middle.empty)

    fun add(e : T) : FunctionalQueue<T> {
        if (head == null) {
            return FunctionalQueue(e, middle, tail)
        } else {
            if (middle != null) {
                return FunctionalQueue(null, middle.add(Pair(e, head as T)), tail)
            } else {
                return FunctionalQueue(null, FunctionalQueue<Pair<T, T>>(Pair(e, head as T), null, null), tail)
            }
        }
    }
    fun removeFirst() : Pair<T, FunctionalQueue<T>> {
        if (tail != null) {
            return Pair(tail as T, FunctionalQueue(head, middle, null))
        } else {
            if (middle == null) {
                if (head == null) {
                    throw java.util.NoSuchElementException()
                } else {
                    return Pair(head as T, FunctionalQueue<T>())
                }
            } else {
                val current = middle.removeFirst()
                if (current.second.empty) {
                    return Pair(current.first.second, FunctionalQueue(head, null, current.first.first))
                } else {
                    return Pair(current.first.second, FunctionalQueue(head, current.second, current.first.first))
                }
            }
        }
    }
}
