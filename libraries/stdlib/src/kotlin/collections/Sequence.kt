package kotlin.sequences

/**
 * A sequence that returns values through its iterator. The values are evaluated lazily, and the sequence
 * is potentially infinite.
 *
 * Sequences can be iterated multiple times, however some sequence implementations might constrain themselves
 * to be iterated only once. That is mentioned specifically in their documentation (e.g. [generateSequence] overload).
 * The latter sequences throw an exception on an attempt to iterate them the second time.
 *
 * Sequence operations, like [Sequence.map], [Sequence.filter] etc, generally preserve that property of a sequence, and
 * again it's documented for an operation if it doesn't.
 *
 * @param T the type of elements in the sequence.
 */
public interface Sequence<out T> {
    /**
     * Returns an [Iterator] that returns the values from the sequence.
     *
     * Throws an exception if the sequence is constrained to be iterated once and `iterator` is invoked the second time.
     */
    public operator fun iterator(): Iterator<T>
}
