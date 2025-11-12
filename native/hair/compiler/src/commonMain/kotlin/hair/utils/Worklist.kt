package hair.utils

// TODO make a proper worklist
class Worklist<T>(elements: Collection<T>? = null) : Iterable<T> {
    private val elements = elements?.let { ArrayDeque(it) } ?: ArrayDeque()

    fun isEmpty() = elements.isEmpty()
    fun isNotEmpty() = elements.isNotEmpty()

    fun add(element: T) {
        elements.add(element)
    }

    fun addAll(elements: Collection<T>) {
        this.elements.addAll(elements)
    }

    fun remove(): T = elements.removeLast()

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        override fun hasNext(): Boolean = isNotEmpty()
        override fun next(): T = remove()
    }
}

fun <T> Collection<T>.toWorklist() = Worklist(this)
fun <T> Sequence<T>.toWorklist() = toList().toWorklist()

fun <T, R> withWorklist(elements: Collection<T>, action: Worklist<T>.() -> R): R {
    val worklist = elements.toWorklist()
    return worklist.action()
}

fun <T, R> withWorklist(elements: Sequence<T>, action: Worklist<T>.() -> R): R = withWorklist(elements.toList(), action)

fun <T> forEachInWorklist(elements: Collection<T>, action: Worklist<T>.(T) -> Unit) {
    withWorklist(elements) {
        forEach { action(it) }
    }
}

fun <T> forEachInWorklist(elements: Sequence<T>, action: Worklist<T>.(T) -> Unit) = forEachInWorklist(elements.toList(), action)
