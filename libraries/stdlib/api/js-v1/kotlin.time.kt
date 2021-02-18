@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.days: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.days: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.days: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.hours: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.hours: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.hours: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.microseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.microseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.microseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.milliseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.milliseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.milliseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.minutes: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.minutes: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.minutes: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.nanoseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.nanoseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.nanoseconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Double.seconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Int.seconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public val kotlin.Long.seconds: kotlin.time.Duration { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public inline fun measureTime(block: () -> kotlin.Unit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public inline fun <T> measureTimedValue(block: () -> T): kotlin.time.TimedValue<T>

@kotlin.time.ExperimentalTime
@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Comparing one TimeMark to another is not a well defined operation because these time marks could have been obtained from the different time sources.")
public inline operator fun kotlin.time.TimeMark.compareTo(other: kotlin.time.TimeMark): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public inline fun kotlin.time.TimeSource.measureTime(block: () -> kotlin.Unit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public inline fun <T> kotlin.time.TimeSource.measureTimedValue(block: () -> T): kotlin.time.TimedValue<T>

@kotlin.time.ExperimentalTime
@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Subtracting one TimeMark from another is not a well defined operation because these time marks could have been obtained from the different time sources.")
public inline operator fun kotlin.time.TimeMark.minus(other: kotlin.time.TimeMark): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun kotlin.Double.times(duration: kotlin.time.Duration): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun kotlin.Int.times(duration: kotlin.time.Duration): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public fun kotlin.Double.toDuration(unit: kotlin.time.DurationUnit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public fun kotlin.Int.toDuration(unit: kotlin.time.DurationUnit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public fun kotlin.Long.toDuration(unit: kotlin.time.DurationUnit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public abstract class AbstractDoubleTimeSource : kotlin.time.TimeSource {
    public constructor AbstractDoubleTimeSource(unit: kotlin.time.DurationUnit)

    protected final val unit: kotlin.time.DurationUnit { get; }

    public open override fun markNow(): kotlin.time.TimeMark

    protected abstract fun read(): kotlin.Double
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public abstract class AbstractLongTimeSource : kotlin.time.TimeSource {
    public constructor AbstractLongTimeSource(unit: kotlin.time.DurationUnit)

    protected final val unit: kotlin.time.DurationUnit { get; }

    public open override fun markNow(): kotlin.time.TimeMark

    protected abstract fun read(): kotlin.Long
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.jvm.JvmInline
public final inline class Duration : kotlin.Comparable<kotlin.time.Duration> {
    public final val absoluteValue: kotlin.time.Duration { get; }

    public final val inDays: kotlin.Double { get; }

    public final val inHours: kotlin.Double { get; }

    public final val inMicroseconds: kotlin.Double { get; }

    public final val inMilliseconds: kotlin.Double { get; }

    public final val inMinutes: kotlin.Double { get; }

    public final val inNanoseconds: kotlin.Double { get; }

    public final val inSeconds: kotlin.Double { get; }

    public open override operator fun compareTo(other: kotlin.time.Duration): kotlin.Int

    public final operator fun div(scale: kotlin.Double): kotlin.time.Duration

    public final operator fun div(scale: kotlin.Int): kotlin.time.Duration

    public final operator fun div(other: kotlin.time.Duration): kotlin.Double

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public final fun isFinite(): kotlin.Boolean

    public final fun isInfinite(): kotlin.Boolean

    public final fun isNegative(): kotlin.Boolean

    public final fun isPositive(): kotlin.Boolean

    public final operator fun minus(other: kotlin.time.Duration): kotlin.time.Duration

    public final operator fun plus(other: kotlin.time.Duration): kotlin.time.Duration

    public final operator fun times(scale: kotlin.Double): kotlin.time.Duration

    public final operator fun times(scale: kotlin.Int): kotlin.time.Duration

    public final inline fun <T> toComponents(action: (days: kotlin.Int, hours: kotlin.Int, minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T

    public final inline fun <T> toComponents(action: (hours: kotlin.Int, minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T

    public final inline fun <T> toComponents(action: (minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T

    public final inline fun <T> toComponents(action: (seconds: kotlin.Long, nanoseconds: kotlin.Int) -> T): T

    public final fun toDouble(unit: kotlin.time.DurationUnit): kotlin.Double

    public final fun toInt(unit: kotlin.time.DurationUnit): kotlin.Int

    public final fun toIsoString(): kotlin.String

    public final fun toLong(unit: kotlin.time.DurationUnit): kotlin.Long

    public final fun toLongMilliseconds(): kotlin.Long

    public final fun toLongNanoseconds(): kotlin.Long

    public open override fun toString(): kotlin.String

    public final fun toString(unit: kotlin.time.DurationUnit, decimals: kotlin.Int = ...): kotlin.String

    public final operator fun unaryMinus(): kotlin.time.Duration

    public companion object of Duration {
        public final val INFINITE: kotlin.time.Duration { get; }

        public final val ZERO: kotlin.time.Duration { get; }

        public final fun convert(value: kotlin.Double, sourceUnit: kotlin.time.DurationUnit, targetUnit: kotlin.time.DurationUnit): kotlin.Double
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public final enum class DurationUnit : kotlin.Enum<kotlin.time.DurationUnit> {
    enum entry NANOSECONDS

    enum entry MICROSECONDS

    enum entry MILLISECONDS

    enum entry SECONDS

    enum entry MINUTES

    enum entry HOURS

    enum entry DAYS
}

@kotlin.Experimental(level = Level.ERROR)
@kotlin.RequiresOptIn(level = Level.ERROR)
@kotlin.annotation.MustBeDocumented
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.SinceKotlin(version = "1.3")
public final annotation class ExperimentalTime : kotlin.Annotation {
    public constructor ExperimentalTime()
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public final class TestTimeSource : kotlin.time.AbstractLongTimeSource {
    public constructor TestTimeSource()

    public final operator fun plusAssign(duration: kotlin.time.Duration): kotlin.Unit

    protected open override fun read(): kotlin.Long
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public abstract class TimeMark {
    public constructor TimeMark()

    public abstract fun elapsedNow(): kotlin.time.Duration

    public final fun hasNotPassedNow(): kotlin.Boolean

    public final fun hasPassedNow(): kotlin.Boolean

    public open operator fun minus(duration: kotlin.time.Duration): kotlin.time.TimeMark

    public open operator fun plus(duration: kotlin.time.Duration): kotlin.time.TimeMark
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public interface TimeSource {
    public abstract fun markNow(): kotlin.time.TimeMark

    public companion object of TimeSource {
    }

    public object Monotonic : kotlin.time.TimeSource {
        public open override fun markNow(): kotlin.time.TimeMark

        public open override fun toString(): kotlin.String
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
public final data class TimedValue<T> {
    public constructor TimedValue<T>(value: T, duration: kotlin.time.Duration)

    public final val duration: kotlin.time.Duration { get; }

    public final val value: T { get; }

    public final operator fun component1(): T

    public final operator fun component2(): kotlin.time.Duration

    public final fun copy(value: T = ..., duration: kotlin.time.Duration = ...): kotlin.time.TimedValue<T>

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use AbstractDoubleTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "AbstractDoubleTimeSource", imports = {"kotlin.time.AbstractDoubleTimeSource"}))
public typealias AbstractDoubleClock = kotlin.time.AbstractDoubleTimeSource

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use AbstractLongTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "AbstractLongTimeSource", imports = {"kotlin.time.AbstractLongTimeSource"}))
public typealias AbstractLongClock = kotlin.time.AbstractLongTimeSource

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use TimeSource interface instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeSource", imports = {"kotlin.time.TimeSource"}))
public typealias Clock = kotlin.time.TimeSource

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use TimeMark class instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeMark", imports = {"kotlin.time.TimeMark"}))
public typealias ClockMark = kotlin.time.TimeMark

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use TimeSource.Monotonic instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeSource.Monotonic", imports = {"kotlin.time.TimeSource"}))
public typealias MonoClock = kotlin.time.TimeSource.Monotonic

@kotlin.SinceKotlin(version = "1.3")
@kotlin.time.ExperimentalTime
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use TestTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "TestTimeSource", imports = {"kotlin.time.TestTimeSource"}))
public typealias TestClock = kotlin.time.TestTimeSource