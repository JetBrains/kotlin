package kotlin.ranges

/**
 * A range of values of an `enum class` type. Permits membership testing and iteration.
 *
 * @see rangeTo
 * @see downTo
 * @see until
 */
@SinceKotlin("1.2")
public class EnumRange<E: Enum<E>>
    @PublishedApi internal constructor(start: E, endInclusive: E, enumValues: Array<E>)
: EnumProgression<E>(start, endInclusive, enumValues, 1), ClosedRange<E> {

    override val start: E get() = first
    override val endInclusive: E get() = last

    override fun contains(value: E): Boolean = first <= value && value <= last

    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
            other is EnumRange<*> &&
                ((isEmpty() && other.isEmpty()) ||
                 (first == other.first && last == other.last))

    override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * first.ordinal + last.ordinal)

    override fun toString(): String = "$first..$last"

    companion object {
        /** An empty range of the specified `enum class` type. */
        public inline fun <reified E: Enum<E>> empty(): EnumRange<E> {
            val enumValues = enumValues<E>()
            if (enumValues.size < 2)
                throw kotlin.IllegalArgumentException("Cannot create an empty range for an enum with less than 2 values")

            return EnumRange(enumValues[1], enumValues[0], enumValues)
        }
    }
}

/**
 * A progression of values of an `enum class` type.
 */
@SinceKotlin("1.2")
public open class EnumProgression<E: Enum<E>>
@PublishedApi internal constructor
(
        start: E,
        endInclusive: E,
        enumValues: Array<E>,
        step: Int
) : Iterable<E> {
    init {
        if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: E = start

    /**
     * The last element in the progression.
     */
    public val last: E = getProgressionLastElement(start, endInclusive, enumValues, step)

    /**
     * The step of the progression.
     */
    public val step: Int = step

    private val enumValues = enumValues.copyOf()

    override fun iterator(): Iterator<E> = EnumProgressionIterator(first, last, enumValues, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
            other is EnumProgression<*> && (isEmpty() && other.isEmpty() ||
                    first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * (31 * first.ordinal + last.ordinal) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    companion object {
        /**
         * Creates EnumProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it,
         * with the specified [step]. In order to go backwards the [step] must be negative.
         */
        public inline fun <reified E: Enum<E>> fromClosedRange(rangeStart: E, rangeEnd: E, step: Int): EnumProgression<E> =
                EnumProgression(rangeStart, rangeEnd, enumValues(), step)
    }
}

/**
 * An iterator over a progression of values of an `enum class` type.
 *
 * @property step the number by which the ordinal of the next enum value to return is incremented on each step.
 */
internal class EnumProgressionIterator<E: Enum<E>>(first: E, last: E, enumValues: Array<E>, val step: Int) : Iterator<E> {
    private val finalElement = last
    private val enumValues = enumValues.copyOf()
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): E {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        }
        else {
            next = enumValues[next.ordinal + step]
        }
        return value
    }
}

/**
 * Creates a range from this `enum class` value to the specified [that] value.
 *
 * This value needs to be smaller than [that] value, otherwise the returned range will be empty.
 *
 * @sample samples.ranges.Ranges.rangeFromEnum
 */
@SinceKotlin("1.2")
public inline operator fun <reified E: Enum<E>> E.rangeTo(that: E): EnumRange<E> = EnumRange(this, that, enumValues())

/**
 * Returns a progression from this `enum class` value down to the specified [to] value of the same class, with the step -1.
 *
 * The [to] value has to be less than this value.
 */
@SinceKotlin("1.2")
public infix inline fun <reified E: Enum<E>> E.downTo(to: E): EnumProgression<E> {
    return EnumProgression.fromClosedRange(this, to, -1)
}

/**
 * Returns a progression that goes over the same range of `enum class` values in the opposite direction with the same step.
 */
@SinceKotlin("1.2")
public inline fun <reified E: Enum<E>> EnumProgression<E>.reversed(): EnumProgression<E> {
    return EnumProgression.fromClosedRange(last, first, -step)
}

/**
 * Returns a range from this `enum class` value up to but excluding the specified [to] value of the same class.
 */
@SinceKotlin("1.2")
public inline infix fun <reified E: Enum<E>> E.until(to: E): EnumRange<E> {
    return when {
        to.ordinal > 0 -> this.rangeTo(enumValues<E>()[to.ordinal - 1])
        enumValues<E>().size > 1 && to <= this -> EnumRange.empty() // Equivalent functionality for 'in' and 'iterator()'
        else -> throw IllegalArgumentException("Cannot create an open EnumRange from ${this} until ${to}")
    }
}

/**
 * Returns a progression that goes over the same `enum class` range with the given [step].
 *
 * @sample samples.ranges.Ranges.enumRangeStep
 */
@SinceKotlin("1.2")
public infix inline fun <reified E: Enum<E>> EnumProgression<E>.step(step: Int): EnumProgression<E> {
    if (!(step > 0)) throw IllegalArgumentException("Step must be positive, was: $step.")
    return EnumProgression.fromClosedRange(first, last, if (this.step > 0) step else -step)
}

internal fun <E: Enum<E>> getProgressionLastElement (start: E, end: E, enumValues: Array<E>, step: Int): E {
    val lastOrdinal = when {
        step == 0 -> throw kotlin.IllegalArgumentException("Step is zero.")
        (step > 0) != (start < end) -> end.ordinal // if step goes opposite direction to range, it's empty; just use same end
        step == 1 || step == -1 -> end.ordinal
        step > 1 -> {
            val distance = end.ordinal - start.ordinal
            val shortfall = distance.rem(step)
            end.ordinal - shortfall
        }
        else /* step < -1 */ -> {
            val distance = start.ordinal - end.ordinal
            val shortfall = distance.rem(step)
            end.ordinal + shortfall
        }
    }
    return enumValues[lastOrdinal]
}
