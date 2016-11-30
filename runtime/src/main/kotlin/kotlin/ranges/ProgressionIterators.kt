package kotlin.ranges

/**
 * An iterator over a progression of values of type `Char`.
 * @property step the number by which the value is incremented on each step.
 */
internal class CharProgressionIterator(first: Char, last: Char, val step: Int) : CharIterator() {
    private var next = first.toInt()
    private val finalElement = last.toInt()
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextChar(): Char {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += step
        }
        return value.toChar()
    }
}

/**
 * An iterator over a progression of values of type `Int`.
 * @property step the number by which the value is incremented on each step.
 */
internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator() {
    private var next = first
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
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

/**
 * An iterator over a progression of values of type `Long`.
 * @property step the number by which the value is incremented on each step.
 */
internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator() {
    private var next = first
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextLong(): Long {
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