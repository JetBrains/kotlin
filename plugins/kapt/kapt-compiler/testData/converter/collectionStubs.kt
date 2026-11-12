// WITH_STDLIB
// FULL_JDK

/** A map with a limit. */
class LimitedHashMap<K, V> : LinkedHashMap<K, V>()

class IdentitySet<T> : MutableSet<T> {
    private val set = LinkedHashSet<T>()
    override val size: Int get() = set.size
    override fun add(element: T): Boolean = set.add(element)
    override fun addAll(elements: Collection<T>): Boolean = set.addAll(elements)
    override fun clear() = set.clear()
    override fun iterator(): MutableIterator<T> = set.iterator()
    override fun remove(element: T): Boolean = set.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = set.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = set.retainAll(elements)
    override fun contains(element: T): Boolean = set.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = set.containsAll(elements)
    override fun isEmpty(): Boolean = set.isEmpty()
}

class StatementIterator<T> : Iterator<T> {
    override fun hasNext(): Boolean = false
    override fun next(): T = throw NoSuchElementException()
}

/** Files of files. */
class FileToFilesMap : HashMap<java.io.File, List<java.io.File>>()

abstract class PersistentMap<K, V> : MutableMap<K, V>

class ErrorDescriptor(val message: String)

class CompilerDiagnostics(private val list: List<ErrorDescriptor>) : List<ErrorDescriptor> by list

abstract class NullableKeys<K, V>(m: MutableMap<K, V>) : MutableMap<K, V> by m

abstract class NonNullKeys<K : Any, V>(m: MutableMap<K, V>) : MutableMap<K, V> by m

class Dependencies(value: List<String>) : MutableList<String> by value.toMutableList()

/** A unique list. */
class UniqueList<E>(list: ArrayList<E>) : java.util.AbstractList<E>(), MutableList<E> {
    override val size: Int get() = 0
    override fun get(index: Int): E = throw IndexOutOfBoundsException()
}

@JvmInline
value class RowId(val id: Long)

class Rows(list: MutableList<RowId>) : MutableList<RowId> by list
