package kotlin.collections

public abstract class AbstractCollection<out E> protected constructor() : Collection<E> {
    abstract override val size: Int
    abstract override fun iterator(): Iterator<E>

    override fun contains(element: @UnsafeVariance E): Boolean = any { it == element }

    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean =
            elements.all { contains(it) }

    override fun isEmpty(): Boolean = size == 0


    override fun toString(): String = joinToString(", ", "[", "]") {
        if (it === this) "(this Collection)" else it.toString()
    }
}
