package kotlin

/**
 * A progression of values of type `Int`.
 */
public open class IntProgression
constructor
(
        start: Int,
        endInclusive: Int,
        val step: Int
) {
    init {
        if (step == 0) {
            println("Step must be non-zero.")
            assert(false)
        }
    }

    /**
     * The first element in the progression.
     */
    public val first: Int = start

    /**
     * The last element in the progression.
     */
    public val last: Int = getProgressionLastElement(start.toInt(), endInclusive.toInt(), step).toInt()


    fun iterator(): IntIterator = IntIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    //[TODO] equals

    override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * (31 * first + last) + step)

    companion object {
        /**
         * Creates IntProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Int, rangeEnd: Int, step: Int): IntProgression = IntProgression(rangeStart, rangeEnd, step)
    }
}
