/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlin.time

import kotlin.internal.ReadObjectParameterType
import kotlin.internal.throwReadObjectNotSupported
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A moment in time.
 *
 * A point in time must be uniquely identified in a way that is independent of a time zone.
 * For example, `1970-01-01, 00:00:00` does not represent a moment in time since this would happen at different times
 * in different time zones: someone in Tokyo would think it is already `1970-01-01` several hours earlier than someone
 * in Berlin would. To represent such entities, use the `LocalDateTime` from `kotlinx-datetime`.
 * In contrast, "the moment the clocks in London first showed 00:00 on Jan 1, 2000" is a specific moment
 * in time, as is "1970-01-01, 00:00:00 UTC+0", so it can be represented as an [Instant].
 *
 * `Instant` uses the UTC-SLS (smeared leap second) time scale. This time scale doesn't contain instants
 * corresponding to leap seconds, but instead "smears" positive and negative leap seconds among the last 1000 seconds
 * of the day when a leap second happens.
 *
 * ### Obtaining the current moment
 *
 * The [Clock] interface is the primary way to obtain the current moment:
 *
 * ```
 * val clock: Clock = Clock.System
 * val instant = clock.now()
 * ```
 *
 * The [Clock.System] implementation uses the platform-specific system clock to obtain the current moment.
 * Note that this clock is not guaranteed to be monotonic, and the user or the system may adjust it at any time,
 * so it should not be used for measuring time intervals.
 * For that, consider using [TimeSource.Monotonic] and [TimeMark] instead of [Clock.System] and [Instant].
 *
 * ### Arithmetic operations
 *
 * The [plus] and [minus] operators can be used to add [Duration]s to and subtract them from an [Instant]:
 *
 * ```
 * Clock.System.now() + 5.seconds // 5 seconds from now
 * ```
 *
 * Also, there is a [minus] operator that returns the [Duration] representing the difference between two instants:
 *
 * ```
 * val kotlinRelease = Instant.parse("2016-02-15T12:00:00+03:00")
 * val kotlinStableDuration = Clock.System.now() - kotlinRelease
 * ```
 *
 * ### Platform specifics
 *
 * On the JVM, there are `Instant.toJavaInstant()` and `java.time.Instant.toKotlinInstant()`
 * extension functions to convert between `kotlin.time` and `java.time` objects used for the same purpose.
 * Likewise, on JS, there are `Instant.toJSDate()` and `Date.toKotlinInstant()` extension functions.
 *
 * For technical reasons, converting [Instant] to and from Foundation's `NSDate` is provided in
 * `kotlinx-datetime` via `Instant.toNSDate()` and `NSDate.toKotlinInstant()` extension functions.
 * These functions might become available in `kotlin.time` in the future.
 *
 * ### Construction, serialization, and deserialization
 *
 * [fromEpochSeconds] can be used to construct an instant from the number of seconds since
 * `1970-01-01T00:00:00Z` (the Unix epoch).
 * [epochSeconds] and [nanosecondsOfSecond] can be used to obtain the number of seconds and nanoseconds since the epoch.
 *
 * ```
 * val instant = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant.epochSeconds // 1709898983
 * instant.nanosecondsOfSecond // 123456789
 * ```
 *
 * [fromEpochMilliseconds] allows constructing an instant from the number of milliseconds since the epoch.
 * [toEpochMilliseconds] can be used to obtain the number of milliseconds since the epoch.
 * Note that [Instant] supports nanosecond precision, so converting to milliseconds is a lossy operation.
 *
 * ```
 * val instant1 = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant1.nanosecondsOfSecond // 123456789
 * val milliseconds = instant1.toEpochMilliseconds() // 1709898983123
 * val instant2 = Instant.fromEpochMilliseconds(milliseconds)
 * instant2.nanosecondsOfSecond // 123000000
 * ```
 *
 * [parse] and [toString] methods can be used to obtain an [Instant] from and convert it to a string in the
 * [ISO 8601 extended format](https://en.wikipedia.org/wiki/ISO_8601#Combined_date_and_time_representations),
 * which includes a time zone designator.
 *
 * ```
 * val instant = Instant.parse("2023-01-02T22:35:01+01:00")
 * instant.toString() // 2023-01-02T21:35:01Z
 * ```
 */
@SinceKotlin("2.3")
@WasExperimental(ExperimentalTime::class)
public class Instant internal constructor(
    /**
     * The number of seconds from the epoch instant `1970-01-01T00:00:00Z` rounded down to a [Long] number.
     *
     * The difference between the rounded number of seconds and the actual number of seconds
     * is returned by [nanosecondsOfSecond] property expressed in nanoseconds.
     *
     * Note that this number doesn't include leap seconds added or removed since the epoch.
     *
     * @see fromEpochSeconds
     * @sample samples.time.Instants.epochSecondsAndNanosecondsOfSecond
     * @sample samples.time.Instants.fromEpochSecondsProperties
     */
    public val epochSeconds: Long,
    /**
     * The number of nanoseconds by which this instant is later than [epochSeconds] from the epoch instant.
     *
     * The value is always non-negative and lies in the range `0..999_999_999`.
     *
     * @see fromEpochSeconds
     * @sample samples.time.Instants.epochSecondsAndNanosecondsOfSecond
     * @sample samples.time.Instants.fromEpochSecondsProperties
     */
    public val nanosecondsOfSecond: Int
) : Comparable<Instant>, Serializable {

    init {
        require(epochSeconds in MIN_SECOND..MAX_SECOND) { "Instant exceeds minimum or maximum instant" }
    }

    /**
     * Returns the number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
     *
     * Any fractional part of a millisecond is rounded toward zero to the whole number of milliseconds.
     *
     * If the result does not fit in [Long],
     * returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
     *
     * @see fromEpochMilliseconds
     * @sample samples.time.Instants.toEpochMilliseconds
     */
    public fun toEpochMilliseconds(): Long {
        if (epochSeconds >= 0) {
            val millis = safeMultiplyOrElse(epochSeconds, MILLIS_PER_SECOND.toLong()) {
                return Long.MAX_VALUE
            }
            return safeAddOrElse(millis, (nanosecondsOfSecond / NANOS_PER_MILLI).toLong()) {
                return Long.MAX_VALUE
            }
        } else {
            // To ensure clamping only when an overflow would occur, first multiply (epochSeconds + 1)
            // by MILLIS_PER_SECOND. This adjustment initially decreases the absolute value of a negative
            // epochSeconds by one. Then, add the whole number of milliseconds from the nanosecondsOfSecond,
            // adjusted by subtracting MILLIS_PER_SECOND to compensate for the initially ignored negative second.
            val millis = safeMultiplyOrElse(epochSeconds + 1, MILLIS_PER_SECOND.toLong()) {
                return Long.MIN_VALUE
            }
            return safeAddOrElse(millis, (nanosecondsOfSecond / NANOS_PER_MILLI - MILLIS_PER_SECOND).toLong()) {
                return Long.MIN_VALUE
            }
        }
    }

    /**
     * Returns an instant that is the result of adding the specified [duration] to this instant.
     *
     * If the [duration] is positive, the returned instant is later than this instant.
     * If the [duration] is negative, the returned instant is earlier than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours, but in some time zones,
     * some days can be shorter or longer because clocks are shifted.
     * Consider using `kotlinx-datetime` for arithmetic operations that take time zone transitions into account.
     *
     * @sample samples.time.Instants.plusDuration
     */
    public operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        if (secondsToAdd == 0L && nanosecondsToAdd == 0) {
            return this
        }
        val newEpochSeconds: Long = safeAddOrElse(epochSeconds, secondsToAdd) {
            return if (duration.isPositive()) MAX else MIN
        }
        // Safe: both values' absolute values are less than 10^9
        val nanoAdjustment = nanosecondsOfSecond + nanosecondsToAdd
        return fromEpochSeconds(newEpochSeconds, nanoAdjustment)
    }

    /**
     * Returns an instant that is the result of subtracting the specified [duration] from this instant.
     *
     * If the [duration] is positive, the returned instant is earlier than this instant.
     * If the [duration] is negative, the returned instant is later than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours, but in some time zones,
     * some days can be shorter or longer because clocks are shifted.
     * Consider using `kotlinx-datetime` for arithmetic operations that take time zone transitions into account.
     *
     * @sample samples.time.Instants.minusDuration
     */
    public operator fun minus(duration: Duration): Instant = plus(-duration)

    /**
     * Returns the [Duration] between two instants: [other] and `this`.
     *
     * The duration returned is positive if this instant is later than the other,
     * and negative if this instant is earlier than the other.
     *
     * The result is never clamped, but note that for instants that are far apart,
     * the value returned may represent the duration between them inexactly due to the loss of precision.
     *
     * Note that sources of [Instant] values (in particular, [Clock]) are not guaranteed to be in sync with each other
     * or even monotonic, so the result of this operation may be negative even if the other instant was observed later
     * than this one, or vice versa.
     * For measuring time intervals, consider using [TimeSource.Monotonic].
     *
     * @sample samples.time.Instants.minusInstant
     */
    public operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
                (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    /**
     * Compares `this` instant with the [other] instant.
     *
     * Returns zero if this instant represents the same moment as the other (meaning they are equal to one another),
     * a negative number if this instant is earlier than the other,
     * and a positive number if this instant is later than the other.
     *
     * @sample samples.time.Instants.compareToSample
     */
    public override operator fun compareTo(other: Instant): Int {
        val s = epochSeconds.compareTo(other.epochSeconds)
        if (s != 0) {
            return s
        }
        return nanosecondsOfSecond.compareTo(other.nanosecondsOfSecond)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Instant && this.epochSeconds == other.epochSeconds
                && this.nanosecondsOfSecond == other.nanosecondsOfSecond

    override fun hashCode(): Int =
        epochSeconds.hashCode() + 51 * nanosecondsOfSecond

    /**
     * Converts this instant to the ISO 8601 string representation, for example, `2023-01-02T23:40:57.120Z`.
     *
     * The representation uses the UTC-SLS time scale instead of UTC.
     * In practice, this means that leap second handling will not be readjusted to the UTC.
     * Leap seconds will not be added or skipped, so it is impossible to acquire a string
     * where the component for seconds is 60, and for any day, it's possible to observe 23:59:59.
     *
     * @see parse
     * @sample samples.time.Instants.toStringSample
     */
    public override fun toString(): String = formatIso(this)

    private fun writeReplace(): Any = serializedInstant(this)

    private fun readObject(input: ReadObjectParameterType): Unit = throwReadObjectNotSupported()

    public companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlin.time.Clock"), level = DeprecationLevel.ERROR)
        public fun now(): Instant = throw NotImplementedError()

        /**
         * Returns an [Instant] that is [epochMilliseconds] number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
         *
         * Every value of [epochMilliseconds] is guaranteed to be representable as an [Instant].
         *
         * Note that [Instant] also supports nanosecond precision via [fromEpochSeconds].
         *
         * @see Instant.toEpochMilliseconds
         * @sample samples.time.Instants.fromEpochMilliseconds
         */
        public fun fromEpochMilliseconds(epochMilliseconds: Long): Instant {
            val epochSeconds = epochMilliseconds.floorDiv(MILLIS_PER_SECOND.toLong())
            val nanosecondsOfSecond = (epochMilliseconds.mod(MILLIS_PER_SECOND.toLong()) * NANOS_PER_MILLI).toInt()
            return when {
                epochSeconds < MIN_SECOND -> MIN
                epochSeconds > MAX_SECOND -> MAX
                else -> fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
            }
        }

        /**
         * Returns an [Instant] that is the [epochSeconds] number of seconds from the epoch instant `1970-01-01T00:00:00Z`
         * and the [nanosecondAdjustment] number of nanoseconds.
         *
         * [nanosecondAdjustment] describes how many nanoseconds the given instant is later than the one defined by just [epochSeconds].
         * For convenience and flexibility, [fromEpochSeconds] accepts [nanosecondAdjustment] values outside
         * of [Instant.nanosecondsOfSecond] range. Negative [nanosecondAdjustment] means that the given instant will be earlier than
         * the one defined by just [epochSeconds], and values greater than a second will contribute to the resulting number of seconds.
         *
         * For example, if [nanosecondAdjustment] is `-1`, the resulting [Instant] will be `1` nanosecond earlier than
         * if [nanosecondAdjustment] was `0`, and if it is `1_000_000_000`, the resulting [Instant] will be `1` second later.
         *
         * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
         * In any case, it is guaranteed that instants between [DISTANT_PAST] and [DISTANT_FUTURE] can be represented.
         *
         * [fromEpochMilliseconds] is a similar function for when input data only has millisecond precision.
         *
         * @see Instant.epochSeconds
         * @see Instant.nanosecondsOfSecond
         * @sample samples.time.Instants.fromEpochSeconds
         * @sample samples.time.Instants.fromEpochSecondsProperties
         */
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long = 0): Instant {
            val seconds = safeAddOrElse(epochSeconds, nanosecondAdjustment.floorDiv(NANOS_PER_SECOND.toLong())) {
                return if (epochSeconds > 0) MAX else MIN
            }
            return when {
                seconds < MIN_SECOND -> MIN
                seconds > MAX_SECOND -> MAX
                else -> {
                    val nanoseconds = nanosecondAdjustment.mod(NANOS_PER_SECOND.toLong()).toInt()
                    Instant(seconds, nanoseconds)
                }
            }
        }

        /**
         * Returns an [Instant] that is the [epochSeconds] number of seconds from the epoch instant `1970-01-01T00:00:00Z`
         * and the [nanosecondAdjustment] number of nanoseconds.
         *
         * [nanosecondAdjustment] describes how many nanoseconds the given instant is later than the one defined by just [epochSeconds].
         * For convenience and flexibility, [fromEpochSeconds] accepts [nanosecondAdjustment] values outside
         * of [Instant.nanosecondsOfSecond] range. Negative [nanosecondAdjustment] means that the given instant will be earlier than
         * the one defined by just [epochSeconds], and values greater than a second will contribute to the resulting number of seconds.
         *
         * For example, if [nanosecondAdjustment] is `-1`, the resulting [Instant] will be `1` nanosecond earlier than
         * if [nanosecondAdjustment] was `0`, and if it is `1_000_000_000`, the resulting [Instant] will be `1` second later.
         *
         * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
         * In any case, it is guaranteed that instants between [DISTANT_PAST] and [DISTANT_FUTURE] can be represented.
         *
         * [fromEpochMilliseconds] is a similar function for when input data only has millisecond precision.
         *
         * @see Instant.epochSeconds
         * @see Instant.nanosecondsOfSecond
         * @sample samples.time.Instants.fromEpochSeconds
         * @sample samples.time.Instants.fromEpochSecondsProperties
         */
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        /**
         * Parses an ISO 8601 string that represents an instant (for example, `2020-08-30T18:43:00Z`).
         *
         * Guaranteed to parse all strings that [Instant.toString] produces.
         *
         * Examples of instants in the ISO 8601 format:
         * - `2020-08-30T18:43:00Z`
         * - `2020-08-30T18:43:00.50Z`
         * - `2020-08-30T18:43:00.123456789Z`
         * - `2020-08-30T18:40:00+03:00`
         * - `2020-08-30T18:40:00+03:30:20`
         * * `2020-01-01T23:59:59.123456789+01`
         * * `+12020-01-31T23:59:59Z`
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         *
         * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
         * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
         * time is parsed, even if it's in the last 1000 seconds of the day.
         * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
         * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
         *
         * @see Instant.parseOrNull for a version of this function that returns `null` on failure.
         * @see Instant.toString for formatting.
         * @sample samples.time.Instants.parsing
         */
        // never null: with throwOnFailure = true, every failure site throws (see handleParseError)
        public fun parse(input: CharSequence): Instant = parseIso(input, throwOnFailure = true)!!

        /**
         * Parses an ISO 8601 string that represents an instant (for example, `2020-08-30T18:43:00Z`),
         * or returns `null` if the string cannot be parsed or the boundaries of [Instant] are exceeded.
         *
         * Guaranteed to parse all strings that [Instant.toString] produces.
         *
         * Examples of instants in the ISO 8601 format:
         * - `2020-08-30T18:43:00Z`
         * - `2020-08-30T18:43:00.50Z`
         * - `2020-08-30T18:43:00.123456789Z`
         * - `2020-08-30T18:40:00+03:00`
         * - `2020-08-30T18:40:00+03:30:20`
         * * `2020-01-01T23:59:59.123456789+01`
         * * `+12020-01-31T23:59:59Z`
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         *
         * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
         * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
         * time is parsed, even if it's in the last 1000 seconds of the day.
         * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
         * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
         *
         * @see Instant.parse for a version of this function that throws an exception on failure.
         * @see Instant.toString for formatting.
         * @sample samples.time.Instants.parseOrNull
         */
        public fun parseOrNull(input: CharSequence): Instant? = parseIso(input, throwOnFailure = false)

        /**
         * An instant value that is far in the past.
         *
         * [isDistantPast] returns true for this value and all earlier ones.
         *
         * @sample samples.time.Instants.isDistantPast
         */
        public val DISTANT_PAST: Instant // -100001-12-31T23:59:59.999999999Z
            get() = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        /**
         * An instant value that is far in the future.
         *
         * [isDistantFuture] returns true for this value and all later ones.
         *
         * @sample samples.time.Instants.isDistantFuture
         */
        public val DISTANT_FUTURE: Instant // +100000-01-01T00:00:00Z
            get() = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)

        internal val MIN = Instant(MIN_SECOND, 0)
        internal val MAX = Instant(MAX_SECOND, 999_999_999)
    }
}

/**
 * Returns true if the instant is [Instant.DISTANT_PAST] or earlier.
 *
 * @sample samples.time.Instants.isDistantPast
 */
@SinceKotlin("2.3")
@WasExperimental(ExperimentalTime::class)
@kotlin.internal.InlineOnly
public inline val Instant.isDistantPast: Boolean
    get() = this <= Instant.DISTANT_PAST

/**
 * Returns true if the instant is [Instant.DISTANT_FUTURE] or later.
 *
 * @sample samples.time.Instants.isDistantFuture
 */
@SinceKotlin("2.3")
@WasExperimental(ExperimentalTime::class)
@kotlin.internal.InlineOnly
public inline val Instant.isDistantFuture: Boolean
    get() = this >= Instant.DISTANT_FUTURE

// internal utilities

internal expect fun serializedInstant(instant: Instant): Any

private const val DISTANT_PAST_SECONDS = -3217862419201
private const val DISTANT_FUTURE_SECONDS = 3093527980800

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31557014167219200L // -1000000000-01-01T00:00:00Z

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31556889864403199L // +1000000000-12-31T23:59:59

private class UnboundLocalDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanosecond: Int,
) {
    override fun toString(): String = "UnboundLocalDateTime($year-$month-$day $hour:$minute:$second.$nanosecond)"

    companion object {
        fun fromInstant(instant: Instant): UnboundLocalDateTime {
            val localSecond: Long = instant.epochSeconds
            val epochDays = localSecond.floorDiv(SECONDS_PER_DAY.toLong())
            val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
            val year: Int
            val month: Int
            val day: Int
            // org.threeten.bp.LocalDate#ofEpochDay
            run {
                var zeroDay = epochDays + DAYS_0000_TO_1970
                // find the march-based year
                zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle

                var adjust = 0L
                if (zeroDay < 0) { // adjust negative years to positive for calculation
                    val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                    adjust = adjustCycles * 400
                    zeroDay += -adjustCycles * DAYS_PER_CYCLE
                }
                var yearEst = ((400 * zeroDay + 591) / DAYS_PER_CYCLE)
                var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                if (doyEst < 0) { // fix estimate
                    yearEst--
                    doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                }
                yearEst += adjust // reset any negative year

                val marchDoy0 = doyEst.toInt()

                // convert march-based values back to january-based
                val marchMonth0 = (marchDoy0 * 5 + 2) / 153
                month = (marchMonth0 + 2) % 12 + 1
                day = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
                year = (yearEst + marchMonth0 / 10).toInt()
            }
            val hours = (secsOfDay / SECONDS_PER_HOUR)
            val secondWithoutHours = secsOfDay - hours * SECONDS_PER_HOUR
            val minutes = (secondWithoutHours / SECONDS_PER_MINUTE)
            val second = secondWithoutHours - minutes * SECONDS_PER_MINUTE
            return UnboundLocalDateTime(year, month, day, hours, minutes, second, instant.nanosecondsOfSecond)
        }
    }
}

private fun epochSecondsFromComponents(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
    offsetSeconds: Int,
): Long {
    // org.threeten.bp.LocalDate#toEpochDay
    val epochDays: Long = run {
        val y = year.toLong()
        var total = 365 * y
        if (y >= 0) {
            // number of leap years since year 0 until (excluding) the given year
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
        } else {
            // number of leap years since the given year until (excluding) year 0
            total -= y / -4 - y / -100 + y / -400
        }
        // number of days since the beginning of a leap year until (excluding)
        // the first day of the given month. Overestimates by 1 for month > 2
        total += ((367 * month - 362) / 12)
        total += day - 1
        if (month > 2) {
            total--
            if (!isLeapYear(year)) {
                total--
            }
        }
        total - DAYS_0000_TO_1970
    }
    val daySeconds = hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
    return epochDays * SECONDS_PER_DAY + daySeconds - offsetSeconds
}

private fun parseIso(isoString: CharSequence, throwOnFailure: Boolean): Instant? {
    // Each expect* helper returns true if the char at [where] matches; otherwise it reports the mismatch via
    // handleParseError, which throws when throwOnFailure and is a no-op returning null otherwise. On the
    // parseOrNull path the message lambda is never invoked, so no error string is built. The checks compare
    // chars directly instead of taking a predicate lambda to keep the hot path free of function dispatch.
    fun expectChar(expected: Char, where: Int): Boolean {
        val c = isoString[where]
        if (c == expected) return true
        handleParseError(throwOnFailure, isoString) { "Expected '$expected', but got '$c' at position $where" }
        return false
    }
    fun expectSeparatorT(where: Int): Boolean {
        val c = isoString[where]
        if (c == 'T' || c == 't') return true
        handleParseError(throwOnFailure, isoString) { "Expected 'T' or 't', but got '$c' at position $where" }
        return false
    }
    fun expectAsciiDigit(where: Int): Boolean {
        val c = isoString[where]
        if (c in '0'..'9') return true
        handleParseError(throwOnFailure, isoString) { "Expected an ASCII digit, but got '$c' at position $where" }
        return false
    }
    val s = isoString
    var i = 0
    if (s.isEmpty()) return handleParseError(throwOnFailure, isoString) { "An empty string is not a valid Instant" }
    val yearSign = when (val c = s[i]) {
        '+', '-' -> { ++i; c }
        else -> ' '
    }
    val yearStart = i
    var absYear = 0
    while (i < s.length && s[i] in '0'..'9') {
        absYear = absYear * 10 + (s[i] - '0')
        ++i
    }
    val yearStrLength = i - yearStart
    val year = when {
        yearStrLength > 10 -> {
            return handleParseError(throwOnFailure, isoString) { "Expected at most 10 digits for the year number, got $yearStrLength digits" }
        }
        yearStrLength == 10 && s[yearStart] >= '2' -> {
            return handleParseError(throwOnFailure, isoString) { "Expected at most 9 digits for the year number or year 1000000000, got $yearStrLength digits" }
        }
        yearStrLength < 4 -> {
            return handleParseError(throwOnFailure, isoString) { "The year number must be padded to 4 digits, got $yearStrLength digits" }
        }
        else -> {
            if (yearSign == '+' && yearStrLength == 4) {
                return handleParseError(throwOnFailure, isoString) { "The '+' sign at the start is only valid for year numbers longer than 4 digits" }
            }
            if (yearSign == ' ' && yearStrLength != 4) {
                return handleParseError(throwOnFailure, isoString) { "A '+' or '-' sign is required for year numbers longer than 4 digits" }
            }
            if (yearSign == '-') -absYear else absYear
        }
    }
    // reading at least -MM-DDTHH:MM:SSZ
    //                  0123456789012345 16 chars
    if (s.length < i + 16) {
        return handleParseError(throwOnFailure, isoString) { "The input string is too short" }
    }
    if (!expectChar('-', i)) return null
    if (!expectChar('-', i + 3)) return null
    if (!expectSeparatorT(i + 6)) return null
    if (!expectChar(':', i + 9)) return null
    if (!expectChar(':', i + 12)) return null
    for (j in asciiDigitPositionsInIsoStringAfterYear) {
        if (!expectAsciiDigit(i + j)) return null
    }
    fun twoDigitNumber(index: Int) = (s[index] - '0') * 10 + (s[index + 1] - '0')
    val month = twoDigitNumber(i + 1)
    val day = twoDigitNumber(i + 4)
    val hour = twoDigitNumber(i + 7)
    val minute = twoDigitNumber(i + 10)
    val second = twoDigitNumber(i + 13)
    val nanosecond = if (s[i + 15] == '.') {
        val fractionStart = i + 16
        i = fractionStart
        var fraction = 0
        while (i < s.length && s[i] in '0'..'9') {
            fraction = fraction * 10 + (s[i] - '0')
            ++i
        }
        val fractionStrLength = i - fractionStart
        if (fractionStrLength in 1..9) {
            fraction * POWERS_OF_TEN[9 - fractionStrLength]
        } else {
            return handleParseError(throwOnFailure, isoString) { "1..9 digits are supported for the fraction of the second, got $fractionStrLength digits" }
        }
    } else {
        i += 15
        0
    }
    if (i >= s.length) {
        return handleParseError(throwOnFailure, isoString) { "The UTC offset at the end of the string is missing" }
    }
    val offsetSeconds = when (val sign = s[i]) {
        'z', 'Z' -> if (s.length == i + 1) {
            0
        } else {
            return handleParseError(throwOnFailure, isoString) { "Extra text after the instant at position ${i + 1}" }
        }
        '-', '+' -> {
            val offsetStrLength = s.length - i
            if (offsetStrLength > 9) {
                return handleParseError(throwOnFailure, isoString) { "The UTC offset string \"${s.substring(i).truncateForErrorMessage(16)}\" is too long" }
            }
            if (offsetStrLength % 3 != 0) { return handleParseError(throwOnFailure, isoString) { "Invalid UTC offset string \"${s.substring(i)}\"" } }
            for (j in colonsInIsoOffsetString) {
                if (i + j >= s.length)
                    break
                if (s[i + j] != ':')
                    return handleParseError(throwOnFailure, isoString) { "Expected ':' at index ${i + j}, got '${s[i + j]}'" }
            }
            for (j in asciiDigitsInIsoOffsetString) {
                if (i + j >= s.length)
                    break
                if (s[i + j] !in '0'..'9')
                    return handleParseError(throwOnFailure, isoString) { "Expected an ASCII digit at index ${i + j}, got '${s[i + j]}'" }
            }
            val offsetHour = twoDigitNumber(i + 1)
            val offsetMinute = if (offsetStrLength > 3) { twoDigitNumber(i + 4) } else { 0 }
            val offsetSecond = if (offsetStrLength > 6) { twoDigitNumber(i + 7) } else { 0 }
            if (offsetMinute > 59) { return handleParseError(throwOnFailure, isoString) { "Expected offset-minute-of-hour in 0..59, got $offsetMinute" } }
            if (offsetSecond > 59) { return handleParseError(throwOnFailure, isoString) { "Expected offset-second-of-minute in 0..59, got $offsetSecond" } }
            if (offsetHour > 17 && !(offsetHour == 18 && offsetMinute == 0 && offsetSecond == 0)) {
                return handleParseError(throwOnFailure, isoString) { "Expected an offset in -18:00..+18:00, got ${s.substring(i)}" }
            }
            (offsetHour * 3600 + offsetMinute * 60 + offsetSecond) * if (sign == '-') -1 else 1
        }
        else -> {
            return handleParseError(throwOnFailure, isoString) { "Expected the UTC offset at position $i, got '$sign'" }
        }
    }
    if (month !in 1..12) { return handleParseError(throwOnFailure, isoString) { "Expected a month number in 1..12, got $month" } }
    if (day !in 1..month.monthLength(isLeapYear(year))) {
        return handleParseError(throwOnFailure, isoString) { "Expected a valid day-of-month for month $month of year $year, got $day" }
    }
    if (hour > 23) { return handleParseError(throwOnFailure, isoString) { "Expected hour in 0..23, got $hour" } }
    if (minute > 59) { return handleParseError(throwOnFailure, isoString) { "Expected minute-of-hour in 0..59, got $minute" } }
    if (second > 59) { return handleParseError(throwOnFailure, isoString) { "Expected second-of-minute in 0..59, got $second" } }
    val epochSeconds = epochSecondsFromComponents(
        year = year, month = month, day = day, hour = hour, minute = minute, second = second, offsetSeconds = offsetSeconds,
    )
    if (epochSeconds < MIN_SECOND || epochSeconds > MAX_SECOND) {
        return handleParseError(throwOnFailure, isoString) {
            "The parsed date is outside the range representable by Instant (Unix epoch second $epochSeconds)"
        }
    }
    return Instant(epochSeconds, nanosecond)
}

private fun formatIso(instant: Instant): String = buildString {
    val ldt = UnboundLocalDateTime.fromInstant(instant)
    fun Appendable.appendTwoDigits(number: Int) {
        if (number < 10) append('0')
        append(number)
    }
    val number = ldt.year
    when {
        number.absoluteValue < 1_000 -> {
            val innerBuilder = StringBuilder()
            if (number >= 0) {
                innerBuilder.append((number + 10_000)).deleteAt(0)
            } else {
                innerBuilder.append((number - 10_000)).deleteAt(1)
            }
            append(innerBuilder)
        }
        else -> {
            if (number >= 10_000) append('+')
            append(number)
        }
    }
    append('-')
    appendTwoDigits(ldt.month)
    append('-')
    appendTwoDigits(ldt.day)
    append('T')
    appendTwoDigits(ldt.hour)
    append(':')
    appendTwoDigits(ldt.minute)
    append(':')
    appendTwoDigits(ldt.second)
    if (ldt.nanosecond != 0) {
        append('.')
        var zerosToStrip = 0
        while (ldt.nanosecond % POWERS_OF_TEN[zerosToStrip + 1] == 0) {
            ++zerosToStrip
        }
        zerosToStrip -= zerosToStrip % 3 // rounding down to a multiple of 3
        val numberToOutput = ldt.nanosecond / POWERS_OF_TEN[zerosToStrip]
        append((numberToOutput + POWERS_OF_TEN[9 - zerosToStrip]).toString().substring(1))
    }
    append('Z')
}

/**
 * The number of days in a 400 year cycle.
 */
private const val DAYS_PER_CYCLE = 146097

/**
 * The number of days from year zero to year 1970.
 * There are five 400 year cycles from year zero to 2000.
 * There are 7 leap years from 1970 to 2000.
 */
private const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5 - (30 * 365 + 7)

/**
 * Safely adds two long values.
 * @throws ArithmeticException if the result overflows a long
 */
private inline fun safeAddOrElse(a: Long, b: Long, action: () -> Nothing): Long {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        action()
    }
    return sum
}

/**
 * Safely multiply a long by a long.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
private inline fun safeMultiplyOrElse(a: Long, b: Long, action: () -> Nothing): Long {
    if (b == 1L) {
        return a
    }
    if (a == 1L) {
        return b
    }
    if (a == 0L || b == 0L) {
        return 0
    }
    val total = a * b
    if (total / b != a || a == Long.MIN_VALUE && b == -1L || b == Long.MIN_VALUE && a == -1L) {
        action()
    }
    return total
}

private const val SECONDS_PER_HOUR = 60 * 60

private const val SECONDS_PER_MINUTE = 60

private const val HOURS_PER_DAY = 24

private const val SECONDS_PER_DAY: Int = SECONDS_PER_HOUR * HOURS_PER_DAY

internal const val NANOS_PER_SECOND = 1_000_000_000
private const val NANOS_PER_MILLI = 1_000_000
private const val MILLIS_PER_SECOND = 1_000

internal fun isLeapYear(year: Int): Boolean {
    return year and 3 == 0 && (year % 100 != 0 || year % 400 == 0)
}

private fun Int.monthLength(isLeapYear: Boolean): Int =
    when (this) {
        2 -> if (isLeapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

private val POWERS_OF_TEN = intArrayOf(
    1,
    10,
    100,
    1000,
    10000,
    100000,
    1000000,
    10000000,
    100000000,
    1000000000
)

private val asciiDigitPositionsInIsoStringAfterYear = intArrayOf(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)
private val colonsInIsoOffsetString = intArrayOf(3, 6)
private val asciiDigitsInIsoOffsetString = intArrayOf(1, 2, 4, 5, 7, 8)

private fun CharSequence.truncateForErrorMessage(maxLength: Int): String {
    return if (length <= maxLength) this.toString() else substring(0, maxLength) + "..."
}

/**
 * The single throw-or-return-null decision point for [parseIso].
 *
 * Kept a top-level `inline` function (not a local one — Kotlin forbids `inline` on local functions) taking a lazy
 * [message]: on the `parseOrNull` path ([throwOnFailure] `== false`) the lambda is never invoked, so no exception,
 * no error string, and no message closure is allocated.
 *
 * Only the site-specific message interpolation is inlined; appending the input suffix and constructing/throwing
 * the exception stay in the non-inline [throwInstantFormatException], so the cold error-reporting code is emitted
 * once instead of at every parse-failure site, keeping [parseIso] small.
 */
@kotlin.internal.InlineOnly
private inline fun handleParseError(throwOnFailure: Boolean, input: CharSequence, message: () -> String): Nothing? {
    if (throwOnFailure) {
        throwInstantFormatException(input, message())
    }
    return null
}

private fun throwInstantFormatException(input: CharSequence, message: String): Nothing {
    throw InstantFormatException("$message when parsing an Instant from \"${input.truncateForErrorMessage(64)}\"")
}

private class InstantFormatException(message: String) : IllegalArgumentException(message)
