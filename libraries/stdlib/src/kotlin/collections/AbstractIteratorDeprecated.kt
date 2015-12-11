package kotlin.support

/**
 * A base class to simplify implementing iterators so that implementations only have to implement [computeNext]
 * to implement the iterator, calling [done] when the iteration is complete.
 */
@Deprecated("Use AbstractIterator from kotlin.collections instead.", ReplaceWith("kotlin.collections.AbstractIterator<T>"))
public abstract class AbstractIterator<T> : kotlin.collections.AbstractIterator<T>()