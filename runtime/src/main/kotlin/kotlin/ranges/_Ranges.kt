package kotlin.ranges

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Int.until(to: Byte): IntRange {
    return this .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Long.until(to: Byte): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Byte.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Short.until(to: Byte): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to ['\u0000'] the returned range is empty.
 */
public infix fun Char.until(to: Char): CharRange {
    if (to <= '\u0000') return CharRange.EMPTY
    return this .. (to - 1).toChar()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Int.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Long.until(to: Int): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Byte.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Int.MIN_VALUE] the returned range is empty.
 */
public infix fun Short.until(to: Int): IntRange {
    if (to <= Int.MIN_VALUE) return IntRange.EMPTY
    return this.toInt() .. (to - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Int.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Long.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Byte.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 *
 * If the [to] value is less than or equal to [Long.MIN_VALUE] the returned range is empty.
 */
public infix fun Short.until(to: Long): LongRange {
    if (to <= Long.MIN_VALUE) return LongRange.EMPTY
    return this.toLong() .. (to - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Int.until(to: Short): IntRange {
    return this .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Long.until(to: Short): LongRange {
    return this .. (to.toLong() - 1).toLong()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Byte.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
public infix fun Short.until(to: Short): IntRange {
    return this.toInt() .. (to.toInt() - 1).toInt()
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Int>.contains(value: Byte): Boolean {
    return contains(value.toInt())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Long>.contains(value: Byte): Boolean {
    return contains(value.toLong())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Short>.contains(value: Byte): Boolean {
    return contains(value.toShort())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Double>.contains(value: Byte): Boolean {
    return contains(value.toDouble())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Float>.contains(value: Byte): Boolean {
    return contains(value.toFloat())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Int>.contains(value: Double): Boolean {
    return value.toIntExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Long>.contains(value: Double): Boolean {
    return value.toLongExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Byte>.contains(value: Double): Boolean {
    return value.toByteExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Short>.contains(value: Double): Boolean {
    return value.toShortExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Float>.contains(value: Double): Boolean {
    return contains(value.toFloat())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Int>.contains(value: Float): Boolean {
    return value.toIntExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Long>.contains(value: Float): Boolean {
    return value.toLongExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Byte>.contains(value: Float): Boolean {
    return value.toByteExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Short>.contains(value: Float): Boolean {
    return value.toShortExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Double>.contains(value: Float): Boolean {
    return contains(value.toDouble())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Long>.contains(value: Int): Boolean {
    return contains(value.toLong())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Byte>.contains(value: Int): Boolean {
    return value.toByteExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Short>.contains(value: Int): Boolean {
    return value.toShortExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Double>.contains(value: Int): Boolean {
    return contains(value.toDouble())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Float>.contains(value: Int): Boolean {
    return contains(value.toFloat())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Int>.contains(value: Long): Boolean {
    return value.toIntExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Byte>.contains(value: Long): Boolean {
    return value.toByteExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Short>.contains(value: Long): Boolean {
    return value.toShortExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Double>.contains(value: Long): Boolean {
    return contains(value.toDouble())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Float>.contains(value: Long): Boolean {
    return contains(value.toFloat())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Int>.contains(value: Short): Boolean {
    return contains(value.toInt())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Long>.contains(value: Short): Boolean {
    return contains(value.toLong())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Byte>.contains(value: Short): Boolean {
    return value.toByteExactOrNull().let { if (it != null) contains(it) else false }
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Double>.contains(value: Short): Boolean {
    return contains(value.toDouble())
}

/**
 * Checks if the specified [value] belongs to this range.
 */
public operator fun ClosedRange<Float>.contains(value: Short): Boolean {
    return contains(value.toFloat())
}

/**
 * Returns a progression from this value down to the specified [to] value with the step -1.
 *
 * The [to] value has to be less than this value.
 */
public infix fun Short.downTo(to: Long): LongProgression {
    return LongProgression.fromClosedRange(this.toLong(), to, -1L)
}

/**
 * Returns a progression that goes over the same range in the opposite direction with the same step.
 */
public fun LongProgression.reversed(): LongProgression {
    return LongProgression.fromClosedRange(last, first, -step)
}

/**
 * Returns a progression that goes over the same range in the opposite direction with the same step.
 */
public fun CharProgression.reversed(): CharProgression {
    return CharProgression.fromClosedRange(last, first, -step)
}

internal fun Int.toByteExactOrNull(): Byte? {
    return if (this in Byte.MIN_VALUE.toInt()..Byte.MAX_VALUE.toInt()) this.toByte() else null
}

internal fun Long.toByteExactOrNull(): Byte? {
    return if (this in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) this.toByte() else null
}

internal fun Short.toByteExactOrNull(): Byte? {
    return if (this in Byte.MIN_VALUE.toShort()..Byte.MAX_VALUE.toShort()) this.toByte() else null
}

internal fun Double.toByteExactOrNull(): Byte? {
    return if (this in Byte.MIN_VALUE.toDouble()..Byte.MAX_VALUE.toDouble()) this.toByte() else null
}

internal fun Float.toByteExactOrNull(): Byte? {
    return if (this in Byte.MIN_VALUE.toFloat()..Byte.MAX_VALUE.toFloat()) this.toByte() else null
}

internal fun Long.toIntExactOrNull(): Int? {
    return if (this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) this.toInt() else null
}

internal fun Double.toIntExactOrNull(): Int? {
    return if (this in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) this.toInt() else null
}

internal fun Float.toIntExactOrNull(): Int? {
    return if (this in Int.MIN_VALUE.toFloat()..Int.MAX_VALUE.toFloat()) this.toInt() else null
}

internal fun Double.toLongExactOrNull(): Long? {
    return if (this in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) this.toLong() else null
}

internal fun Float.toLongExactOrNull(): Long? {
    return if (this in Long.MIN_VALUE.toFloat()..Long.MAX_VALUE.toFloat()) this.toLong() else null
}

internal fun Int.toShortExactOrNull(): Short? {
    return if (this in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt()) this.toShort() else null
}

internal fun Long.toShortExactOrNull(): Short? {
    return if (this in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) this.toShort() else null
}

internal fun Double.toShortExactOrNull(): Short? {
    return if (this in Short.MIN_VALUE.toDouble()..Short.MAX_VALUE.toDouble()) this.toShort() else null
}

internal fun Float.toShortExactOrNull(): Short? {
    return if (this in Short.MIN_VALUE.toFloat()..Short.MAX_VALUE.toFloat()) this.toShort() else null
}

/**
 * Ensures that this value lies in the specified [range].
 *
 * @return this value if it's in the [range], or `range.start` if this value is less than `range.start`, or `range.endInclusive` if this value is greater than `range.endInclusive`.
 */
public fun <T: Comparable<T>> T.coerceIn(range: ClosedFloatingPointRange<T>): T {
    if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: $range.")
    return when {
    // this < start equiv to this <= start && !(this >= start)
        range.lessThanOrEquals(this, range.start) && !range.lessThanOrEquals(range.start, this) -> range.start
    // this > end equiv to this >= end && !(this <= end)
        range.lessThanOrEquals(range.endInclusive, this) && !range.lessThanOrEquals(this, range.endInclusive) -> range.endInclusive
        else -> this
    }
}
