package hair.utils

class ChangeTracker {
    private var changed = true
    fun changed() { changed = true }
    fun isChanged() = changed
    internal fun reset() { changed = false }
}

fun whileChanged(limit: Int? = null, action: ChangeTracker.(() -> Unit) -> Unit) {
    var iter = 0
    val tracker = ChangeTracker()
    while (tracker.isChanged()) {
        if (iter == limit) error("Looping too long")
        tracker.reset()
        tracker.action { TODO() }
        iter++
    }
}

fun <T> whileChanged(watchedValue: () -> T, action: () -> Unit) = whileChanged {
    val oldValue = watchedValue()
    action()
    val newValue = watchedValue()
    if (newValue != oldValue) changed()
}

fun <T> Iterable<T>.closure(expand: (T) -> Iterable<T>): Set<T> {
    val closure = linkedSetOf<T>()
    closure.addAll(this)
    whileChanged({ closure.size }) {
        for (x in closure.toList()) {
            closure.addAll(expand(x))
        }
    }
    return closure.toSet()
}

fun <T> closure(seed: T, expand: (T) -> Iterable<T>): Set<T> = listOf(seed).closure(expand)

// FIXME???
fun <T> Iterable<T>.dfsClosure(expand: (T) -> Iterable<T>): Set<T> {
    val closure = linkedSetOf<T>()
    fun visit(n: T) {
        if (n in closure) return
        closure += n
        for (succ in expand(n)) {
            visit(succ)
        }
    }
    this.forEach { visit(it) }
    return closure
}

fun shouldNotReachHere(obj: Any? = null): Nothing = error("Should not reach here" + (obj?.let { ": $obj" } ?: ""))

fun <T> List<List<T>>.transpose(): List<List<T>> {
    if (this.isEmpty()) return emptyList()
    val rowSize = first().size
    require(all { it.size == rowSize })

    return List(rowSize) { col ->
        this.map { row -> row[col] }
    }
}

class TotalMap<K, V>(private val data: Map<K, V>) : Map<K, V> by data {
    override operator fun get(key: K): V = data[key]!!
}
fun <K, V> Map<K, V>.asTotalMap() = TotalMap(this.toMap())

