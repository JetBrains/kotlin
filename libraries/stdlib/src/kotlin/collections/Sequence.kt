package kotlin.sequences

/**
 * A sequence that returns values through its iterator. The values are evaluated lazily, and the sequence
 * is potentially infinite.
 *
 * @param T the type of elements in the sequence.
 */
public interface Sequence<out T> {
    /**
     * Returns an iterator that returns the values from the sequence.
     */
    public operator fun iterator(): Iterator<T>
}
