package kotlin.time

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.days: kotlin.time.Duration
    public fun kotlin.Double.<get-days>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.days: kotlin.time.Duration
    public fun kotlin.Int.<get-days>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.days: kotlin.time.Duration
    public fun kotlin.Long.<get-days>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.hours: kotlin.time.Duration
    public fun kotlin.Double.<get-hours>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.hours: kotlin.time.Duration
    public fun kotlin.Int.<get-hours>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.hours: kotlin.time.Duration
    public fun kotlin.Long.<get-hours>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.microseconds: kotlin.time.Duration
    public fun kotlin.Double.<get-microseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.microseconds: kotlin.time.Duration
    public fun kotlin.Int.<get-microseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.microseconds: kotlin.time.Duration
    public fun kotlin.Long.<get-microseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.milliseconds: kotlin.time.Duration
    public fun kotlin.Double.<get-milliseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.milliseconds: kotlin.time.Duration
    public fun kotlin.Int.<get-milliseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.milliseconds: kotlin.time.Duration
    public fun kotlin.Long.<get-milliseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.minutes: kotlin.time.Duration
    public fun kotlin.Double.<get-minutes>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.minutes: kotlin.time.Duration
    public fun kotlin.Int.<get-minutes>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.minutes: kotlin.time.Duration
    public fun kotlin.Long.<get-minutes>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.nanoseconds: kotlin.time.Duration
    public fun kotlin.Double.<get-nanoseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.nanoseconds: kotlin.time.Duration
    public fun kotlin.Int.<get-nanoseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.nanoseconds: kotlin.time.Duration
    public fun kotlin.Long.<get-nanoseconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Double.seconds: kotlin.time.Duration
    public fun kotlin.Double.<get-seconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Int.seconds: kotlin.time.Duration
    public fun kotlin.Int.<get-seconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public val kotlin.Long.seconds: kotlin.time.Duration
    public fun kotlin.Long.<get-seconds>(): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public inline fun measureTime(/*0*/ block: () -> kotlin.Unit): kotlin.time.Duration
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public inline fun </*0*/ T> measureTimedValue(/*0*/ block: () -> T): kotlin.time.TimedValue<T>
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.time.ExperimentalTime @kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Comparing one TimeMark to another is not a well defined operation because these time marks could have been obtained from the different time sources.") public inline operator fun kotlin.time.TimeMark.compareTo(/*0*/ other: kotlin.time.TimeMark): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public inline fun kotlin.time.TimeSource.measureTime(/*0*/ block: () -> kotlin.Unit): kotlin.time.Duration
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public inline fun </*0*/ T> kotlin.time.TimeSource.measureTimedValue(/*0*/ block: () -> T): kotlin.time.TimedValue<T>
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.time.ExperimentalTime @kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Subtracting one TimeMark from another is not a well defined operation because these time marks could have been obtained from the different time sources.") public inline operator fun kotlin.time.TimeMark.minus(/*0*/ other: kotlin.time.TimeMark): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.internal.InlineOnly public inline operator fun kotlin.Double.times(/*0*/ duration: kotlin.time.Duration): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.internal.InlineOnly public inline operator fun kotlin.Int.times(/*0*/ duration: kotlin.time.Duration): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public fun kotlin.Double.toDuration(/*0*/ unit: kotlin.time.DurationUnit): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public fun kotlin.Int.toDuration(/*0*/ unit: kotlin.time.DurationUnit): kotlin.time.Duration
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public fun kotlin.Long.toDuration(/*0*/ unit: kotlin.time.DurationUnit): kotlin.time.Duration

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public abstract class AbstractDoubleTimeSource : kotlin.time.TimeSource {
    /*primary*/ public constructor AbstractDoubleTimeSource(/*0*/ unit: kotlin.time.DurationUnit)
    protected final val unit: kotlin.time.DurationUnit
        protected final fun <get-unit>(): kotlin.time.DurationUnit
    public open override /*1*/ fun markNow(): kotlin.time.TimeMark
    protected abstract fun read(): kotlin.Double
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public abstract class AbstractLongTimeSource : kotlin.time.TimeSource {
    /*primary*/ public constructor AbstractLongTimeSource(/*0*/ unit: kotlin.time.DurationUnit)
    protected final val unit: kotlin.time.DurationUnit
        protected final fun <get-unit>(): kotlin.time.DurationUnit
    public open override /*1*/ fun markNow(): kotlin.time.TimeMark
    protected abstract fun read(): kotlin.Long
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public final inline class Duration : kotlin.Comparable<kotlin.time.Duration> {
    public final val absoluteValue: kotlin.time.Duration
        public final fun <get-absoluteValue>(): kotlin.time.Duration
    public final val inDays: kotlin.Double
        public final fun <get-inDays>(): kotlin.Double
    public final val inHours: kotlin.Double
        public final fun <get-inHours>(): kotlin.Double
    public final val inMicroseconds: kotlin.Double
        public final fun <get-inMicroseconds>(): kotlin.Double
    public final val inMilliseconds: kotlin.Double
        public final fun <get-inMilliseconds>(): kotlin.Double
    public final val inMinutes: kotlin.Double
        public final fun <get-inMinutes>(): kotlin.Double
    public final val inNanoseconds: kotlin.Double
        public final fun <get-inNanoseconds>(): kotlin.Double
    public final val inSeconds: kotlin.Double
        public final fun <get-inSeconds>(): kotlin.Double
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.time.Duration): kotlin.Int
    public final operator fun div(/*0*/ scale: kotlin.Double): kotlin.time.Duration
    public final operator fun div(/*0*/ scale: kotlin.Int): kotlin.time.Duration
    public final operator fun div(/*0*/ other: kotlin.time.Duration): kotlin.Double
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public final fun isFinite(): kotlin.Boolean
    public final fun isInfinite(): kotlin.Boolean
    public final fun isNegative(): kotlin.Boolean
    public final fun isPositive(): kotlin.Boolean
    public final operator fun minus(/*0*/ other: kotlin.time.Duration): kotlin.time.Duration
    public final operator fun plus(/*0*/ other: kotlin.time.Duration): kotlin.time.Duration
    public final operator fun times(/*0*/ scale: kotlin.Double): kotlin.time.Duration
    public final operator fun times(/*0*/ scale: kotlin.Int): kotlin.time.Duration
    public final inline fun </*0*/ T> toComponents(/*0*/ action: (days: kotlin.Int, hours: kotlin.Int, minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T
    public final inline fun </*0*/ T> toComponents(/*0*/ action: (hours: kotlin.Int, minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T
    public final inline fun </*0*/ T> toComponents(/*0*/ action: (minutes: kotlin.Int, seconds: kotlin.Int, nanoseconds: kotlin.Int) -> T): T
    public final inline fun </*0*/ T> toComponents(/*0*/ action: (seconds: kotlin.Long, nanoseconds: kotlin.Int) -> T): T
    public final fun toDouble(/*0*/ unit: kotlin.time.DurationUnit): kotlin.Double
    public final fun toInt(/*0*/ unit: kotlin.time.DurationUnit): kotlin.Int
    public final fun toIsoString(): kotlin.String
    public final fun toLong(/*0*/ unit: kotlin.time.DurationUnit): kotlin.Long
    public final fun toLongMilliseconds(): kotlin.Long
    public final fun toLongNanoseconds(): kotlin.Long
    public open override /*1*/ fun toString(): kotlin.String
    public final fun toString(/*0*/ unit: kotlin.time.DurationUnit, /*1*/ decimals: kotlin.Int = ...): kotlin.String
    public final operator fun unaryMinus(): kotlin.time.Duration

    public companion object Companion {
        public final val INFINITE: kotlin.time.Duration
            public final fun <get-INFINITE>(): kotlin.time.Duration
        public final val ZERO: kotlin.time.Duration
            public final fun <get-ZERO>(): kotlin.time.Duration
        public final fun convert(/*0*/ value: kotlin.Double, /*1*/ sourceUnit: kotlin.time.DurationUnit, /*2*/ targetUnit: kotlin.time.DurationUnit): kotlin.Double
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public final enum class DurationUnit : kotlin.Enum<kotlin.time.DurationUnit> {
    enum entry NANOSECONDS

    enum entry MICROSECONDS

    enum entry MILLISECONDS

    enum entry SECONDS

    enum entry MINUTES

    enum entry HOURS

    enum entry DAYS

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.time.DurationUnit
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.time.DurationUnit>
}

@kotlin.Experimental(level = Level.ERROR) @kotlin.RequiresOptIn(level = Level.ERROR) @kotlin.annotation.MustBeDocumented @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS}) @kotlin.SinceKotlin(version = "1.3") public final annotation class ExperimentalTime : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalTime()
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public final class TestTimeSource : kotlin.time.AbstractLongTimeSource {
    /*primary*/ public constructor TestTimeSource()
    public final operator fun plusAssign(/*0*/ duration: kotlin.time.Duration): kotlin.Unit
    protected open override /*1*/ fun read(): kotlin.Long
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public abstract class TimeMark {
    /*primary*/ public constructor TimeMark()
    public abstract fun elapsedNow(): kotlin.time.Duration
    public final fun hasNotPassedNow(): kotlin.Boolean
    public final fun hasPassedNow(): kotlin.Boolean
    public open operator fun minus(/*0*/ duration: kotlin.time.Duration): kotlin.time.TimeMark
    public open operator fun plus(/*0*/ duration: kotlin.time.Duration): kotlin.time.TimeMark
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public interface TimeSource {
    public abstract fun markNow(): kotlin.time.TimeMark

    public companion object Companion {
    }

    public object Monotonic : kotlin.time.TimeSource {
        public open override /*1*/ /*delegation*/ fun markNow(): kotlin.time.TimeMark
        public open override /*1*/ fun toString(): kotlin.String
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime public final data class TimedValue</*0*/ T> {
    /*primary*/ public constructor TimedValue</*0*/ T>(/*0*/ value: T, /*1*/ duration: kotlin.time.Duration)
    public final val duration: kotlin.time.Duration
        public final fun <get-duration>(): kotlin.time.Duration
    public final val value: T
        public final fun <get-value>(): T
    public final operator /*synthesized*/ fun component1(): T
    public final operator /*synthesized*/ fun component2(): kotlin.time.Duration
    public final /*synthesized*/ fun copy(/*0*/ value: T = ..., /*1*/ duration: kotlin.time.Duration = ...): kotlin.time.TimedValue<T>
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use AbstractDoubleTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "AbstractDoubleTimeSource", imports = {"kotlin.time.AbstractDoubleTimeSource"})) public typealias AbstractDoubleClock = kotlin.time.AbstractDoubleTimeSource
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use AbstractLongTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "AbstractLongTimeSource", imports = {"kotlin.time.AbstractLongTimeSource"})) public typealias AbstractLongClock = kotlin.time.AbstractLongTimeSource
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use TimeSource interface instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeSource", imports = {"kotlin.time.TimeSource"})) public typealias Clock = kotlin.time.TimeSource
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use TimeMark class instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeMark", imports = {"kotlin.time.TimeMark"})) public typealias ClockMark = kotlin.time.TimeMark
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use TimeSource.Monotonic instead.", replaceWith = kotlin.ReplaceWith(expression = "TimeSource.Monotonic", imports = {"kotlin.time.TimeSource"})) public typealias MonoClock = kotlin.time.TimeSource.Monotonic
@kotlin.SinceKotlin(version = "1.3") @kotlin.time.ExperimentalTime @kotlin.Deprecated(message = "Use TestTimeSource instead.", replaceWith = kotlin.ReplaceWith(expression = "TestTimeSource", imports = {"kotlin.time.TestTimeSource"})) public typealias TestClock = kotlin.time.TestTimeSource