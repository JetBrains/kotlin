package kotlin.ranges

public fun </*0*/ T : kotlin.Comparable<T>> T.coerceAtLeast(/*0*/ minimumValue: T): T
public fun kotlin.Byte.coerceAtLeast(/*0*/ minimumValue: kotlin.Byte): kotlin.Byte
public fun kotlin.Double.coerceAtLeast(/*0*/ minimumValue: kotlin.Double): kotlin.Double
public fun kotlin.Float.coerceAtLeast(/*0*/ minimumValue: kotlin.Float): kotlin.Float
public fun kotlin.Int.coerceAtLeast(/*0*/ minimumValue: kotlin.Int): kotlin.Int
public fun kotlin.Long.coerceAtLeast(/*0*/ minimumValue: kotlin.Long): kotlin.Long
public fun kotlin.Short.coerceAtLeast(/*0*/ minimumValue: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByte.coerceAtLeast(/*0*/ minimumValue: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UInt.coerceAtLeast(/*0*/ minimumValue: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULong.coerceAtLeast(/*0*/ minimumValue: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShort.coerceAtLeast(/*0*/ minimumValue: kotlin.UShort): kotlin.UShort
public fun </*0*/ T : kotlin.Comparable<T>> T.coerceAtMost(/*0*/ maximumValue: T): T
public fun kotlin.Byte.coerceAtMost(/*0*/ maximumValue: kotlin.Byte): kotlin.Byte
public fun kotlin.Double.coerceAtMost(/*0*/ maximumValue: kotlin.Double): kotlin.Double
public fun kotlin.Float.coerceAtMost(/*0*/ maximumValue: kotlin.Float): kotlin.Float
public fun kotlin.Int.coerceAtMost(/*0*/ maximumValue: kotlin.Int): kotlin.Int
public fun kotlin.Long.coerceAtMost(/*0*/ maximumValue: kotlin.Long): kotlin.Long
public fun kotlin.Short.coerceAtMost(/*0*/ maximumValue: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByte.coerceAtMost(/*0*/ maximumValue: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UInt.coerceAtMost(/*0*/ maximumValue: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULong.coerceAtMost(/*0*/ maximumValue: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShort.coerceAtMost(/*0*/ maximumValue: kotlin.UShort): kotlin.UShort
public fun </*0*/ T : kotlin.Comparable<T>> T.coerceIn(/*0*/ minimumValue: T?, /*1*/ maximumValue: T?): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T : kotlin.Comparable<T>> T.coerceIn(/*0*/ range: kotlin.ranges.ClosedFloatingPointRange<T>): T
public fun </*0*/ T : kotlin.Comparable<T>> T.coerceIn(/*0*/ range: kotlin.ranges.ClosedRange<T>): T
public fun kotlin.Byte.coerceIn(/*0*/ minimumValue: kotlin.Byte, /*1*/ maximumValue: kotlin.Byte): kotlin.Byte
public fun kotlin.Double.coerceIn(/*0*/ minimumValue: kotlin.Double, /*1*/ maximumValue: kotlin.Double): kotlin.Double
public fun kotlin.Float.coerceIn(/*0*/ minimumValue: kotlin.Float, /*1*/ maximumValue: kotlin.Float): kotlin.Float
public fun kotlin.Int.coerceIn(/*0*/ minimumValue: kotlin.Int, /*1*/ maximumValue: kotlin.Int): kotlin.Int
public fun kotlin.Int.coerceIn(/*0*/ range: kotlin.ranges.ClosedRange<kotlin.Int>): kotlin.Int
public fun kotlin.Long.coerceIn(/*0*/ minimumValue: kotlin.Long, /*1*/ maximumValue: kotlin.Long): kotlin.Long
public fun kotlin.Long.coerceIn(/*0*/ range: kotlin.ranges.ClosedRange<kotlin.Long>): kotlin.Long
public fun kotlin.Short.coerceIn(/*0*/ minimumValue: kotlin.Short, /*1*/ maximumValue: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByte.coerceIn(/*0*/ minimumValue: kotlin.UByte, /*1*/ maximumValue: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UInt.coerceIn(/*0*/ minimumValue: kotlin.UInt, /*1*/ maximumValue: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UInt.coerceIn(/*0*/ range: kotlin.ranges.ClosedRange<kotlin.UInt>): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULong.coerceIn(/*0*/ minimumValue: kotlin.ULong, /*1*/ maximumValue: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULong.coerceIn(/*0*/ range: kotlin.ranges.ClosedRange<kotlin.ULong>): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShort.coerceIn(/*0*/ minimumValue: kotlin.UShort, /*1*/ maximumValue: kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline operator fun </*0*/ T : kotlin.Any, /*1*/ R : kotlin.collections.Iterable<T>> R.contains(/*0*/ element: T?): kotlin.Boolean where R : kotlin.ranges.ClosedRange<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline operator fun kotlin.ranges.CharRange.contains(/*0*/ element: kotlin.Char?): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "byteRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(/*0*/ value: kotlin.Double): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "byteRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(/*0*/ value: kotlin.Float): kotlin.Boolean
@kotlin.jvm.JvmName(name = "byteRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(/*0*/ value: kotlin.Int): kotlin.Boolean
@kotlin.jvm.JvmName(name = "byteRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(/*0*/ value: kotlin.Long): kotlin.Boolean
@kotlin.jvm.JvmName(name = "byteRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(/*0*/ value: kotlin.Short): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "doubleRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(/*0*/ value: kotlin.Byte): kotlin.Boolean
@kotlin.jvm.JvmName(name = "doubleRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(/*0*/ value: kotlin.Float): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "doubleRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(/*0*/ value: kotlin.Int): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "doubleRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(/*0*/ value: kotlin.Long): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "doubleRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(/*0*/ value: kotlin.Short): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "floatRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(/*0*/ value: kotlin.Byte): kotlin.Boolean
@kotlin.jvm.JvmName(name = "floatRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(/*0*/ value: kotlin.Double): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "floatRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(/*0*/ value: kotlin.Int): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "floatRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(/*0*/ value: kotlin.Long): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "floatRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(/*0*/ value: kotlin.Short): kotlin.Boolean
@kotlin.jvm.JvmName(name = "intRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(/*0*/ value: kotlin.Byte): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "intRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(/*0*/ value: kotlin.Double): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "intRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(/*0*/ value: kotlin.Float): kotlin.Boolean
@kotlin.jvm.JvmName(name = "intRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(/*0*/ value: kotlin.Long): kotlin.Boolean
@kotlin.jvm.JvmName(name = "intRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(/*0*/ value: kotlin.Short): kotlin.Boolean
@kotlin.jvm.JvmName(name = "longRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(/*0*/ value: kotlin.Byte): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "longRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(/*0*/ value: kotlin.Double): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "longRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(/*0*/ value: kotlin.Float): kotlin.Boolean
@kotlin.jvm.JvmName(name = "longRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(/*0*/ value: kotlin.Int): kotlin.Boolean
@kotlin.jvm.JvmName(name = "longRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(/*0*/ value: kotlin.Short): kotlin.Boolean
@kotlin.jvm.JvmName(name = "shortRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(/*0*/ value: kotlin.Byte): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "shortRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(/*0*/ value: kotlin.Double): kotlin.Boolean
@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.") @kotlin.jvm.JvmName(name = "shortRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(/*0*/ value: kotlin.Float): kotlin.Boolean
@kotlin.jvm.JvmName(name = "shortRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(/*0*/ value: kotlin.Int): kotlin.Boolean
@kotlin.jvm.JvmName(name = "shortRangeContains") public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(/*0*/ value: kotlin.Long): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline operator fun kotlin.ranges.IntRange.contains(/*0*/ element: kotlin.Int?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline operator fun kotlin.ranges.LongRange.contains(/*0*/ element: kotlin.Long?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.UIntRange.contains(/*0*/ value: kotlin.UByte): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ranges.UIntRange.contains(/*0*/ element: kotlin.UInt?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.UIntRange.contains(/*0*/ value: kotlin.ULong): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.UIntRange.contains(/*0*/ value: kotlin.UShort): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.ULongRange.contains(/*0*/ value: kotlin.UByte): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.ULongRange.contains(/*0*/ value: kotlin.UInt): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ranges.ULongRange.contains(/*0*/ element: kotlin.ULong?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ranges.ULongRange.contains(/*0*/ value: kotlin.UShort): kotlin.Boolean
public infix fun kotlin.Byte.downTo(/*0*/ to: kotlin.Byte): kotlin.ranges.IntProgression
public infix fun kotlin.Byte.downTo(/*0*/ to: kotlin.Int): kotlin.ranges.IntProgression
public infix fun kotlin.Byte.downTo(/*0*/ to: kotlin.Long): kotlin.ranges.LongProgression
public infix fun kotlin.Byte.downTo(/*0*/ to: kotlin.Short): kotlin.ranges.IntProgression
public infix fun kotlin.Char.downTo(/*0*/ to: kotlin.Char): kotlin.ranges.CharProgression
public infix fun kotlin.Int.downTo(/*0*/ to: kotlin.Byte): kotlin.ranges.IntProgression
public infix fun kotlin.Int.downTo(/*0*/ to: kotlin.Int): kotlin.ranges.IntProgression
public infix fun kotlin.Int.downTo(/*0*/ to: kotlin.Long): kotlin.ranges.LongProgression
public infix fun kotlin.Int.downTo(/*0*/ to: kotlin.Short): kotlin.ranges.IntProgression
public infix fun kotlin.Long.downTo(/*0*/ to: kotlin.Byte): kotlin.ranges.LongProgression
public infix fun kotlin.Long.downTo(/*0*/ to: kotlin.Int): kotlin.ranges.LongProgression
public infix fun kotlin.Long.downTo(/*0*/ to: kotlin.Long): kotlin.ranges.LongProgression
public infix fun kotlin.Long.downTo(/*0*/ to: kotlin.Short): kotlin.ranges.LongProgression
public infix fun kotlin.Short.downTo(/*0*/ to: kotlin.Byte): kotlin.ranges.IntProgression
public infix fun kotlin.Short.downTo(/*0*/ to: kotlin.Int): kotlin.ranges.IntProgression
public infix fun kotlin.Short.downTo(/*0*/ to: kotlin.Long): kotlin.ranges.LongProgression
public infix fun kotlin.Short.downTo(/*0*/ to: kotlin.Short): kotlin.ranges.IntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UByte.downTo(/*0*/ to: kotlin.UByte): kotlin.ranges.UIntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UInt.downTo(/*0*/ to: kotlin.UInt): kotlin.ranges.UIntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ULong.downTo(/*0*/ to: kotlin.ULong): kotlin.ranges.ULongProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UShort.downTo(/*0*/ to: kotlin.UShort): kotlin.ranges.UIntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ranges.CharRange.random(): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ranges.CharRange.random(/*0*/ random: kotlin.random.Random): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ranges.IntRange.random(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ranges.IntRange.random(/*0*/ random: kotlin.random.Random): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ranges.LongRange.random(): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ranges.LongRange.random(/*0*/ random: kotlin.random.Random): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ranges.UIntRange.random(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.UIntRange.random(/*0*/ random: kotlin.random.Random): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ranges.ULongRange.random(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.ULongRange.random(/*0*/ random: kotlin.random.Random): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.ranges.CharRange.randomOrNull(): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ranges.CharRange.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.ranges.IntRange.randomOrNull(): kotlin.Int?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ranges.IntRange.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Int?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.ranges.LongRange.randomOrNull(): kotlin.Long?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ranges.LongRange.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Long?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ranges.UIntRange.randomOrNull(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.UIntRange.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ranges.ULongRange.randomOrNull(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.ULongRange.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.ULong?
public operator fun </*0*/ T : kotlin.Comparable<T>> T.rangeTo(/*0*/ that: T): kotlin.ranges.ClosedRange<T>
@kotlin.SinceKotlin(version = "1.1") public operator fun kotlin.Double.rangeTo(/*0*/ that: kotlin.Double): kotlin.ranges.ClosedFloatingPointRange<kotlin.Double>
@kotlin.SinceKotlin(version = "1.1") public operator fun kotlin.Float.rangeTo(/*0*/ that: kotlin.Float): kotlin.ranges.ClosedFloatingPointRange<kotlin.Float>
public fun kotlin.ranges.CharProgression.reversed(): kotlin.ranges.CharProgression
public fun kotlin.ranges.IntProgression.reversed(): kotlin.ranges.IntProgression
public fun kotlin.ranges.LongProgression.reversed(): kotlin.ranges.LongProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.UIntProgression.reversed(): kotlin.ranges.UIntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ranges.ULongProgression.reversed(): kotlin.ranges.ULongProgression
public infix fun kotlin.ranges.CharProgression.step(/*0*/ step: kotlin.Int): kotlin.ranges.CharProgression
public infix fun kotlin.ranges.IntProgression.step(/*0*/ step: kotlin.Int): kotlin.ranges.IntProgression
public infix fun kotlin.ranges.LongProgression.step(/*0*/ step: kotlin.Long): kotlin.ranges.LongProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ranges.UIntProgression.step(/*0*/ step: kotlin.Int): kotlin.ranges.UIntProgression
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ranges.ULongProgression.step(/*0*/ step: kotlin.Long): kotlin.ranges.ULongProgression
public infix fun kotlin.Byte.until(/*0*/ to: kotlin.Byte): kotlin.ranges.IntRange
public infix fun kotlin.Byte.until(/*0*/ to: kotlin.Int): kotlin.ranges.IntRange
public infix fun kotlin.Byte.until(/*0*/ to: kotlin.Long): kotlin.ranges.LongRange
public infix fun kotlin.Byte.until(/*0*/ to: kotlin.Short): kotlin.ranges.IntRange
public infix fun kotlin.Char.until(/*0*/ to: kotlin.Char): kotlin.ranges.CharRange
public infix fun kotlin.Int.until(/*0*/ to: kotlin.Byte): kotlin.ranges.IntRange
public infix fun kotlin.Int.until(/*0*/ to: kotlin.Int): kotlin.ranges.IntRange
public infix fun kotlin.Int.until(/*0*/ to: kotlin.Long): kotlin.ranges.LongRange
public infix fun kotlin.Int.until(/*0*/ to: kotlin.Short): kotlin.ranges.IntRange
public infix fun kotlin.Long.until(/*0*/ to: kotlin.Byte): kotlin.ranges.LongRange
public infix fun kotlin.Long.until(/*0*/ to: kotlin.Int): kotlin.ranges.LongRange
public infix fun kotlin.Long.until(/*0*/ to: kotlin.Long): kotlin.ranges.LongRange
public infix fun kotlin.Long.until(/*0*/ to: kotlin.Short): kotlin.ranges.LongRange
public infix fun kotlin.Short.until(/*0*/ to: kotlin.Byte): kotlin.ranges.IntRange
public infix fun kotlin.Short.until(/*0*/ to: kotlin.Int): kotlin.ranges.IntRange
public infix fun kotlin.Short.until(/*0*/ to: kotlin.Long): kotlin.ranges.LongRange
public infix fun kotlin.Short.until(/*0*/ to: kotlin.Short): kotlin.ranges.IntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UByte.until(/*0*/ to: kotlin.UByte): kotlin.ranges.UIntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UInt.until(/*0*/ to: kotlin.UInt): kotlin.ranges.UIntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ULong.until(/*0*/ to: kotlin.ULong): kotlin.ranges.ULongRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UShort.until(/*0*/ to: kotlin.UShort): kotlin.ranges.UIntRange

public open class CharProgression : kotlin.collections.Iterable<kotlin.Char> {
    public final val first: kotlin.Char
        public final fun <get-first>(): kotlin.Char
    public final val last: kotlin.Char
        public final fun <get-last>(): kotlin.Char
    public final val step: kotlin.Int
        public final fun <get-step>(): kotlin.Int
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public open fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.CharIterator
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun fromClosedRange(/*0*/ rangeStart: kotlin.Char, /*1*/ rangeEnd: kotlin.Char, /*2*/ step: kotlin.Int): kotlin.ranges.CharProgression
    }
}

public final class CharRange : kotlin.ranges.CharProgression, kotlin.ranges.ClosedRange<kotlin.Char> {
    /*primary*/ public constructor CharRange(/*0*/ start: kotlin.Char, /*1*/ endInclusive: kotlin.Char)
    public open override /*1*/ val endInclusive: kotlin.Char
        public open override /*1*/ fun <get-endInclusive>(): kotlin.Char
    public open override /*1*/ val start: kotlin.Char
        public open override /*1*/ fun <get-start>(): kotlin.Char
    public open override /*1*/ fun contains(/*0*/ value: kotlin.Char): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val EMPTY: kotlin.ranges.CharRange
            public final fun <get-EMPTY>(): kotlin.ranges.CharRange
    }
}

@kotlin.SinceKotlin(version = "1.1") public interface ClosedFloatingPointRange</*0*/ T : kotlin.Comparable<T>> : kotlin.ranges.ClosedRange<T> {
    public open override /*1*/ fun contains(/*0*/ value: T): kotlin.Boolean
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public abstract fun lessThanOrEquals(/*0*/ a: T, /*1*/ b: T): kotlin.Boolean
}

public interface ClosedRange</*0*/ T : kotlin.Comparable<T>> {
    public abstract val endInclusive: T
        public abstract fun <get-endInclusive>(): T
    public abstract val start: T
        public abstract fun <get-start>(): T
    public open operator fun contains(/*0*/ value: T): kotlin.Boolean
    public open fun isEmpty(): kotlin.Boolean
}

public open class IntProgression : kotlin.collections.Iterable<kotlin.Int> {
    public final val first: kotlin.Int
        public final fun <get-first>(): kotlin.Int
    public final val last: kotlin.Int
        public final fun <get-last>(): kotlin.Int
    public final val step: kotlin.Int
        public final fun <get-step>(): kotlin.Int
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public open fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.IntIterator
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun fromClosedRange(/*0*/ rangeStart: kotlin.Int, /*1*/ rangeEnd: kotlin.Int, /*2*/ step: kotlin.Int): kotlin.ranges.IntProgression
    }
}

public final class IntRange : kotlin.ranges.IntProgression, kotlin.ranges.ClosedRange<kotlin.Int> {
    /*primary*/ public constructor IntRange(/*0*/ start: kotlin.Int, /*1*/ endInclusive: kotlin.Int)
    public open override /*1*/ val endInclusive: kotlin.Int
        public open override /*1*/ fun <get-endInclusive>(): kotlin.Int
    public open override /*1*/ val start: kotlin.Int
        public open override /*1*/ fun <get-start>(): kotlin.Int
    public open override /*1*/ fun contains(/*0*/ value: kotlin.Int): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val EMPTY: kotlin.ranges.IntRange
            public final fun <get-EMPTY>(): kotlin.ranges.IntRange
    }
}

public open class LongProgression : kotlin.collections.Iterable<kotlin.Long> {
    public final val first: kotlin.Long
        public final fun <get-first>(): kotlin.Long
    public final val last: kotlin.Long
        public final fun <get-last>(): kotlin.Long
    public final val step: kotlin.Long
        public final fun <get-step>(): kotlin.Long
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public open fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.LongIterator
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun fromClosedRange(/*0*/ rangeStart: kotlin.Long, /*1*/ rangeEnd: kotlin.Long, /*2*/ step: kotlin.Long): kotlin.ranges.LongProgression
    }
}

public final class LongRange : kotlin.ranges.LongProgression, kotlin.ranges.ClosedRange<kotlin.Long> {
    /*primary*/ public constructor LongRange(/*0*/ start: kotlin.Long, /*1*/ endInclusive: kotlin.Long)
    public open override /*1*/ val endInclusive: kotlin.Long
        public open override /*1*/ fun <get-endInclusive>(): kotlin.Long
    public open override /*1*/ val start: kotlin.Long
        public open override /*1*/ fun <get-start>(): kotlin.Long
    public open override /*1*/ fun contains(/*0*/ value: kotlin.Long): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val EMPTY: kotlin.ranges.LongRange
            public final fun <get-EMPTY>(): kotlin.ranges.LongRange
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public open class UIntProgression : kotlin.collections.Iterable<kotlin.UInt> {
    public final val first: kotlin.UInt
        public final fun <get-first>(): kotlin.UInt
    public final val last: kotlin.UInt
        public final fun <get-last>(): kotlin.UInt
    public final val step: kotlin.Int
        public final fun <get-step>(): kotlin.Int
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public open fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.UIntIterator
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun fromClosedRange(/*0*/ rangeStart: kotlin.UInt, /*1*/ rangeEnd: kotlin.UInt, /*2*/ step: kotlin.Int): kotlin.ranges.UIntProgression
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final class UIntRange : kotlin.ranges.UIntProgression, kotlin.ranges.ClosedRange<kotlin.UInt> {
    /*primary*/ public constructor UIntRange(/*0*/ start: kotlin.UInt, /*1*/ endInclusive: kotlin.UInt)
    public open override /*1*/ val endInclusive: kotlin.UInt
        public open override /*1*/ fun <get-endInclusive>(): kotlin.UInt
    public open override /*1*/ val start: kotlin.UInt
        public open override /*1*/ fun <get-start>(): kotlin.UInt
    public open override /*1*/ fun contains(/*0*/ value: kotlin.UInt): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val EMPTY: kotlin.ranges.UIntRange
            public final fun <get-EMPTY>(): kotlin.ranges.UIntRange
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public open class ULongProgression : kotlin.collections.Iterable<kotlin.ULong> {
    public final val first: kotlin.ULong
        public final fun <get-first>(): kotlin.ULong
    public final val last: kotlin.ULong
        public final fun <get-last>(): kotlin.ULong
    public final val step: kotlin.Long
        public final fun <get-step>(): kotlin.Long
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public open fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.ULongIterator
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun fromClosedRange(/*0*/ rangeStart: kotlin.ULong, /*1*/ rangeEnd: kotlin.ULong, /*2*/ step: kotlin.Long): kotlin.ranges.ULongProgression
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final class ULongRange : kotlin.ranges.ULongProgression, kotlin.ranges.ClosedRange<kotlin.ULong> {
    /*primary*/ public constructor ULongRange(/*0*/ start: kotlin.ULong, /*1*/ endInclusive: kotlin.ULong)
    public open override /*1*/ val endInclusive: kotlin.ULong
        public open override /*1*/ fun <get-endInclusive>(): kotlin.ULong
    public open override /*1*/ val start: kotlin.ULong
        public open override /*1*/ fun <get-start>(): kotlin.ULong
    public open override /*1*/ fun contains(/*0*/ value: kotlin.ULong): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val EMPTY: kotlin.ranges.ULongRange
            public final fun <get-EMPTY>(): kotlin.ranges.ULongRange
    }
}