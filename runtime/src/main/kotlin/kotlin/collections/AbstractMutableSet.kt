package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableSet] interface.
 *
 * @param E the type of elements contained in the set. The set is invariant in its element type.
 */
@SinceKotlin("1.1")
public actual abstract class AbstractMutableSet<E> protected actual constructor() : AbstractMutableCollection<E>(), MutableSet<E> {
    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    actual abstract override fun add(element: E): Boolean

    /**
     * Compares this set with another set instance with the unordered structural equality.
     *
     * @return `true`, if [other] instance is a [Set] of the same size, all elements of which are contained in this set.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return AbstractSet.setEquals(this, other)
    }

    /**
     * Returns the hash code value for this set.
     */
    override fun hashCode(): Int = AbstractSet.unorderedHashCode(this)
}