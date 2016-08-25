package kotlin.collections

/** An iterator over a sequence of values of type `Byte`. */
public abstract class ByteIterator {
    final fun next() = nextByte()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextByte(): Byte

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Char`. */
public abstract class CharIterator {
    final fun next() = nextChar()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextChar(): Char

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Short`. */
public abstract class ShortIterator {
    final fun next() = nextShort()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextShort(): Short

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Int`. */
class IntIterator(first: Int, last: Int, val step: Int) {
    private var next = first
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    fun hasNext(): Boolean = hasNext

    fun nextInt(): Int {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}


/** An iterator over a sequence of values of type `Long`. */
public abstract class LongIterator {
    final fun next() = nextLong()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextLong(): Long

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Float`. */
public abstract class FloatIterator {
    final fun next() = nextFloat()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextFloat(): Float

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Double`. */
public abstract class DoubleIterator {
    final fun next() = nextDouble()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextDouble(): Double

    public abstract fun hasNext(): Boolean
}

/** An iterator over a sequence of values of type `Boolean`. */
public abstract class BooleanIterator {
    final fun next() = nextBoolean()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextBoolean(): Boolean

    public abstract fun hasNext(): Boolean
}