package kotlin.collections

/** An iterator over a sequence of values of type `Byte`. */
public abstract class ByteIterator : Iterator<Byte> {
    override final fun next() = nextByte()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextByte(): Byte
}

/** An iterator over a sequence of values of type `Char`. */
public abstract class CharIterator : Iterator<Char> {
    override final fun next() = nextChar()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextChar(): Char
}

/** An iterator over a sequence of values of type `Short`. */
public abstract class ShortIterator : Iterator<Short> {
    override final fun next() = nextShort()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextShort(): Short
}

/** An iterator over a sequence of values of type `Int`. */
public abstract class IntIterator : Iterator<Int> {
    override final fun next() = nextInt()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextInt(): Int
}

/** An iterator over a sequence of values of type `Long`. */
public abstract class LongIterator : Iterator<Long> {
    override final fun next() = nextLong()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextLong(): Long
}

/** An iterator over a sequence of values of type `Float`. */
public abstract class FloatIterator : Iterator<Float> {
    override final fun next() = nextFloat()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextFloat(): Float
}

/** An iterator over a sequence of values of type `Double`. */
public abstract class DoubleIterator : Iterator<Double> {
    override final fun next() = nextDouble()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextDouble(): Double
}

/** An iterator over a sequence of values of type `Boolean`. */
public abstract class BooleanIterator : Iterator<Boolean> {
    override final fun next() = nextBoolean()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextBoolean(): Boolean
}

/**
 * Returns the given iterator itself. This allows to use an instance of iterator in a `for` loop.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Iterator<T>.iterator(): Iterator<T> = this

/**
 * Returns an [Iterator] wrapping each value produced by this [Iterator] with the [IndexedValue],
 * containing value and it's index.
 */
public fun <T> Iterator<T>.withIndex(): Iterator<IndexedValue<T>> = IndexingIterator(this)

/**
 * Performs the given [operation] on each element of this [Iterator].
 */
public inline fun <T> Iterator<T>.forEach(operation: (T) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Iterator transforming original `iterator` into iterator of [IndexedValue], counting index from zero.
 */
internal class IndexingIterator<out T>(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
    private var index = 0
    final override fun hasNext(): Boolean = iterator.hasNext()
    final override fun next(): IndexedValue<T> = IndexedValue(index++, iterator.next())
}