package kotlin.collections

class HashSet<K> internal constructor(
        val backing: HashMap<K, *>
) : MutableSet<K> {

    constructor() : this(HashMap<K, Nothing>())

    constructor(capacity: Int) : this(HashMap<K, Nothing>(capacity))

    constructor(c: Collection<K>) : this(c.size) {
        addAll(c)
    }

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: K): Boolean = backing.containsKey(element)
    override fun clear() = backing.clear()
    override fun add(element: K): Boolean = backing.addKey(element) >= 0
    override fun remove(element: K): Boolean = backing.removeKey(element) >= 0
    override fun iterator(): MutableIterator<K> = backing.keysIterator()

    override fun containsAll(elements: Collection<K>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next()))
                return false
        }
        return true
    }

    override fun addAll(elements: Collection<K>): Boolean {
        val it = elements.iterator()
        var updated = false
        while (it.hasNext()) {
            if (add(it.next()))
                updated = true
        }
        return updated
    }

    override fun removeAll(elements: Collection<K>): Boolean {
        val it = elements.iterator()
        var updated = false
        while (it.hasNext()) {
            if (remove(it.next()))
                updated = true
        }
        return updated
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        val it = iterator()
        var updated = false
        while (it.hasNext()) {
            if (!elements.contains(it.next())) {
                it.remove()
                updated = true
            }
        }
        return updated
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is Set<*>) &&
                        contentEquals(
                                @Suppress("UNCHECKED_CAST") (other as Set<K>))
    }

    override fun hashCode(): Int {
        var result = 0
        val it = iterator()
        while (it.hasNext()) {
            result += it.next().hashCode()
        }
        return result
    }

    override fun toString(): String = collectionToString()

    // ---------------------------- private ----------------------------

    private fun contentEquals(other: Set<K>): Boolean = size == other.size && containsAll(other)
}

// This hash set keeps insertion order.
typealias LinkedHashSet<V> = HashSet<V>