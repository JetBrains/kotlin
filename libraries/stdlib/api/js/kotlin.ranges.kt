public fun <T : kotlin.Comparable<T>> T.coerceAtLeast(minimumValue: T): T

public fun kotlin.Byte.coerceAtLeast(minimumValue: kotlin.Byte): kotlin.Byte

public fun kotlin.Double.coerceAtLeast(minimumValue: kotlin.Double): kotlin.Double

public fun kotlin.Float.coerceAtLeast(minimumValue: kotlin.Float): kotlin.Float

public fun kotlin.Int.coerceAtLeast(minimumValue: kotlin.Int): kotlin.Int

public fun kotlin.Long.coerceAtLeast(minimumValue: kotlin.Long): kotlin.Long

public fun kotlin.Short.coerceAtLeast(minimumValue: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UByte.coerceAtLeast(minimumValue: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UInt.coerceAtLeast(minimumValue: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ULong.coerceAtLeast(minimumValue: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UShort.coerceAtLeast(minimumValue: kotlin.UShort): kotlin.UShort

public fun <T : kotlin.Comparable<T>> T.coerceAtMost(maximumValue: T): T

public fun kotlin.Byte.coerceAtMost(maximumValue: kotlin.Byte): kotlin.Byte

public fun kotlin.Double.coerceAtMost(maximumValue: kotlin.Double): kotlin.Double

public fun kotlin.Float.coerceAtMost(maximumValue: kotlin.Float): kotlin.Float

public fun kotlin.Int.coerceAtMost(maximumValue: kotlin.Int): kotlin.Int

public fun kotlin.Long.coerceAtMost(maximumValue: kotlin.Long): kotlin.Long

public fun kotlin.Short.coerceAtMost(maximumValue: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UByte.coerceAtMost(maximumValue: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UInt.coerceAtMost(maximumValue: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ULong.coerceAtMost(maximumValue: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UShort.coerceAtMost(maximumValue: kotlin.UShort): kotlin.UShort

public fun <T : kotlin.Comparable<T>> T.coerceIn(minimumValue: T?, maximumValue: T?): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T : kotlin.Comparable<T>> T.coerceIn(range: kotlin.ranges.ClosedFloatingPointRange<T>): T

public fun <T : kotlin.Comparable<T>> T.coerceIn(range: kotlin.ranges.ClosedRange<T>): T

public fun kotlin.Byte.coerceIn(minimumValue: kotlin.Byte, maximumValue: kotlin.Byte): kotlin.Byte

public fun kotlin.Double.coerceIn(minimumValue: kotlin.Double, maximumValue: kotlin.Double): kotlin.Double

public fun kotlin.Float.coerceIn(minimumValue: kotlin.Float, maximumValue: kotlin.Float): kotlin.Float

public fun kotlin.Int.coerceIn(minimumValue: kotlin.Int, maximumValue: kotlin.Int): kotlin.Int

public fun kotlin.Int.coerceIn(range: kotlin.ranges.ClosedRange<kotlin.Int>): kotlin.Int

public fun kotlin.Long.coerceIn(minimumValue: kotlin.Long, maximumValue: kotlin.Long): kotlin.Long

public fun kotlin.Long.coerceIn(range: kotlin.ranges.ClosedRange<kotlin.Long>): kotlin.Long

public fun kotlin.Short.coerceIn(minimumValue: kotlin.Short, maximumValue: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UByte.coerceIn(minimumValue: kotlin.UByte, maximumValue: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UInt.coerceIn(minimumValue: kotlin.UInt, maximumValue: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UInt.coerceIn(range: kotlin.ranges.ClosedRange<kotlin.UInt>): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ULong.coerceIn(minimumValue: kotlin.ULong, maximumValue: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ULong.coerceIn(range: kotlin.ranges.ClosedRange<kotlin.ULong>): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UShort.coerceIn(minimumValue: kotlin.UShort, maximumValue: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline operator fun <T : kotlin.Any, R : kotlin.ranges.ClosedRange<T>> R.contains(element: T?): kotlin.Boolean where R : kotlin.collections.Iterable<T>

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline operator fun <T : kotlin.Any, R : kotlin.ranges.OpenEndRange<T>> R.contains(element: T?): kotlin.Boolean where R : kotlin.collections.Iterable<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.CharRange.contains(element: kotlin.Char?): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "byteRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(value: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "byteRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Byte>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "doubleRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.jvm.JvmName(name = "doubleRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "doubleRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "doubleRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "doubleRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Double>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "floatRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.jvm.JvmName(name = "floatRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(value: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "floatRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "floatRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "floatRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Float>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "intRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(value: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "intRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Int>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "longRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(value: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "longRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Long>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "shortRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(value: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "This `contains` operation mixing integer and floating point arguments has ambiguous semantics and is going to be removed.")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.4", hiddenSince = "1.5", warningSince = "1.3")
@kotlin.jvm.JvmName(name = "shortRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
public operator fun kotlin.ranges.ClosedRange<kotlin.Short>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.IntRange.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.IntRange.contains(element: kotlin.Int?): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.IntRange.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.IntRange.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.LongRange.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.LongRange.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.LongRange.contains(element: kotlin.Long?): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.LongRange.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Byte>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Byte>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.jvm.JvmName(name = "byteRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Byte>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "doubleRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Double>.contains(value: kotlin.Float): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Int>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Int>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.jvm.JvmName(name = "intRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Int>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Long>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Long>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "longRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Long>.contains(value: kotlin.Short): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Short>.contains(value: kotlin.Byte): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Short>.contains(value: kotlin.Int): kotlin.Boolean

@kotlin.jvm.JvmName(name = "shortRangeContains")
@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.ranges.OpenEndRange<kotlin.Short>.contains(value: kotlin.Long): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.UIntRange.contains(value: kotlin.UByte): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.UIntRange.contains(element: kotlin.UInt?): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.UIntRange.contains(value: kotlin.ULong): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.UIntRange.contains(value: kotlin.UShort): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.ULongRange.contains(value: kotlin.UByte): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.ULongRange.contains(value: kotlin.UInt): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ranges.ULongRange.contains(element: kotlin.ULong?): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public operator fun kotlin.ranges.ULongRange.contains(value: kotlin.UShort): kotlin.Boolean

public infix fun kotlin.Byte.downTo(to: kotlin.Byte): kotlin.ranges.IntProgression

public infix fun kotlin.Byte.downTo(to: kotlin.Int): kotlin.ranges.IntProgression

public infix fun kotlin.Byte.downTo(to: kotlin.Long): kotlin.ranges.LongProgression

public infix fun kotlin.Byte.downTo(to: kotlin.Short): kotlin.ranges.IntProgression

public infix fun kotlin.Char.downTo(to: kotlin.Char): kotlin.ranges.CharProgression

public infix fun kotlin.Int.downTo(to: kotlin.Byte): kotlin.ranges.IntProgression

public infix fun kotlin.Int.downTo(to: kotlin.Int): kotlin.ranges.IntProgression

public infix fun kotlin.Int.downTo(to: kotlin.Long): kotlin.ranges.LongProgression

public infix fun kotlin.Int.downTo(to: kotlin.Short): kotlin.ranges.IntProgression

public infix fun kotlin.Long.downTo(to: kotlin.Byte): kotlin.ranges.LongProgression

public infix fun kotlin.Long.downTo(to: kotlin.Int): kotlin.ranges.LongProgression

public infix fun kotlin.Long.downTo(to: kotlin.Long): kotlin.ranges.LongProgression

public infix fun kotlin.Long.downTo(to: kotlin.Short): kotlin.ranges.LongProgression

public infix fun kotlin.Short.downTo(to: kotlin.Byte): kotlin.ranges.IntProgression

public infix fun kotlin.Short.downTo(to: kotlin.Int): kotlin.ranges.IntProgression

public infix fun kotlin.Short.downTo(to: kotlin.Long): kotlin.ranges.LongProgression

public infix fun kotlin.Short.downTo(to: kotlin.Short): kotlin.ranges.IntProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UByte.downTo(to: kotlin.UByte): kotlin.ranges.UIntProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UInt.downTo(to: kotlin.UInt): kotlin.ranges.UIntProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.ULong.downTo(to: kotlin.ULong): kotlin.ranges.ULongProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UShort.downTo(to: kotlin.UShort): kotlin.ranges.UIntProgression

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.CharProgression.first(): kotlin.Char

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.IntProgression.first(): kotlin.Int

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.LongProgression.first(): kotlin.Long

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.UIntProgression.first(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.ULongProgression.first(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.CharProgression.firstOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.IntProgression.firstOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.LongProgression.firstOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.UIntProgression.firstOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.ULongProgression.firstOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.CharProgression.last(): kotlin.Char

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.IntProgression.last(): kotlin.Int

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.LongProgression.last(): kotlin.Long

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.UIntProgression.last(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.ULongProgression.last(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.CharProgression.lastOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.IntProgression.lastOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.LongProgression.lastOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.UIntProgression.lastOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.7")
public fun kotlin.ranges.ULongProgression.lastOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.CharRange.random(): kotlin.Char

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ranges.CharRange.random(random: kotlin.random.Random): kotlin.Char

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.IntRange.random(): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ranges.IntRange.random(random: kotlin.random.Random): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.LongRange.random(): kotlin.Long

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ranges.LongRange.random(random: kotlin.random.Random): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.UIntRange.random(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.UIntRange.random(random: kotlin.random.Random): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.ULongRange.random(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.ULongRange.random(random: kotlin.random.Random): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.CharRange.randomOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ranges.CharRange.randomOrNull(random: kotlin.random.Random): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.IntRange.randomOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ranges.IntRange.randomOrNull(random: kotlin.random.Random): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.LongRange.randomOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ranges.LongRange.randomOrNull(random: kotlin.random.Random): kotlin.Long?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.UIntRange.randomOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.UIntRange.randomOrNull(random: kotlin.random.Random): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ranges.ULongRange.randomOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.ULongRange.randomOrNull(random: kotlin.random.Random): kotlin.ULong?

public operator fun <T : kotlin.Comparable<T>> T.rangeTo(that: T): kotlin.ranges.ClosedRange<T>

@kotlin.SinceKotlin(version = "1.1")
public operator fun kotlin.Double.rangeTo(that: kotlin.Double): kotlin.ranges.ClosedFloatingPointRange<kotlin.Double>

@kotlin.SinceKotlin(version = "1.1")
public operator fun kotlin.Float.rangeTo(that: kotlin.Float): kotlin.ranges.ClosedFloatingPointRange<kotlin.Float>

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun <T : kotlin.Comparable<T>> T.rangeUntil(that: T): kotlin.ranges.OpenEndRange<T>

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.Double.rangeUntil(that: kotlin.Double): kotlin.ranges.OpenEndRange<kotlin.Double>

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public operator fun kotlin.Float.rangeUntil(that: kotlin.Float): kotlin.ranges.OpenEndRange<kotlin.Float>

public fun kotlin.ranges.CharProgression.reversed(): kotlin.ranges.CharProgression

public fun kotlin.ranges.IntProgression.reversed(): kotlin.ranges.IntProgression

public fun kotlin.ranges.LongProgression.reversed(): kotlin.ranges.LongProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.UIntProgression.reversed(): kotlin.ranges.UIntProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ranges.ULongProgression.reversed(): kotlin.ranges.ULongProgression

public infix fun kotlin.ranges.CharProgression.step(step: kotlin.Int): kotlin.ranges.CharProgression

public infix fun kotlin.ranges.IntProgression.step(step: kotlin.Int): kotlin.ranges.IntProgression

public infix fun kotlin.ranges.LongProgression.step(step: kotlin.Long): kotlin.ranges.LongProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.ranges.UIntProgression.step(step: kotlin.Int): kotlin.ranges.UIntProgression

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.ranges.ULongProgression.step(step: kotlin.Long): kotlin.ranges.ULongProgression

public infix fun kotlin.Byte.until(to: kotlin.Byte): kotlin.ranges.IntRange

public infix fun kotlin.Byte.until(to: kotlin.Int): kotlin.ranges.IntRange

public infix fun kotlin.Byte.until(to: kotlin.Long): kotlin.ranges.LongRange

public infix fun kotlin.Byte.until(to: kotlin.Short): kotlin.ranges.IntRange

public infix fun kotlin.Char.until(to: kotlin.Char): kotlin.ranges.CharRange

public infix fun kotlin.Int.until(to: kotlin.Byte): kotlin.ranges.IntRange

public infix fun kotlin.Int.until(to: kotlin.Int): kotlin.ranges.IntRange

public infix fun kotlin.Int.until(to: kotlin.Long): kotlin.ranges.LongRange

public infix fun kotlin.Int.until(to: kotlin.Short): kotlin.ranges.IntRange

public infix fun kotlin.Long.until(to: kotlin.Byte): kotlin.ranges.LongRange

public infix fun kotlin.Long.until(to: kotlin.Int): kotlin.ranges.LongRange

public infix fun kotlin.Long.until(to: kotlin.Long): kotlin.ranges.LongRange

public infix fun kotlin.Long.until(to: kotlin.Short): kotlin.ranges.LongRange

public infix fun kotlin.Short.until(to: kotlin.Byte): kotlin.ranges.IntRange

public infix fun kotlin.Short.until(to: kotlin.Int): kotlin.ranges.IntRange

public infix fun kotlin.Short.until(to: kotlin.Long): kotlin.ranges.LongRange

public infix fun kotlin.Short.until(to: kotlin.Short): kotlin.ranges.IntRange

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UByte.until(to: kotlin.UByte): kotlin.ranges.UIntRange

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UInt.until(to: kotlin.UInt): kotlin.ranges.UIntRange

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.ULong.until(to: kotlin.ULong): kotlin.ranges.ULongRange

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public infix fun kotlin.UShort.until(to: kotlin.UShort): kotlin.ranges.UIntRange

public open class CharProgression : kotlin.collections.Iterable<kotlin.Char> {
    public final val first: kotlin.Char { get; }

    public final val last: kotlin.Char { get; }

    public final val step: kotlin.Int { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.CharIterator

    public open override fun toString(): kotlin.String

    public companion object of CharProgression {
        public final fun fromClosedRange(rangeStart: kotlin.Char, rangeEnd: kotlin.Char, step: kotlin.Int): kotlin.ranges.CharProgression
    }
}

public final class CharRange : kotlin.ranges.CharProgression, kotlin.ranges.ClosedRange<kotlin.Char>, kotlin.ranges.OpenEndRange<kotlin.Char> {
    public constructor CharRange(start: kotlin.Char, endInclusive: kotlin.Char)

    @kotlin.Deprecated(message = "Can throw an exception when it's impossible to represent the value with Char type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @kotlin.SinceKotlin(version = "1.9")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public open override val endExclusive: kotlin.Char { get; }

    public open override val endInclusive: kotlin.Char { get; }

    public open override val start: kotlin.Char { get; }

    public open override operator fun contains(value: kotlin.Char): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of CharRange {
        public final val EMPTY: kotlin.ranges.CharRange { get; }
    }
}

@kotlin.SinceKotlin(version = "1.1")
public interface ClosedFloatingPointRange<T : kotlin.Comparable<T>> : kotlin.ranges.ClosedRange<T> {
    public open override operator fun contains(value: T): kotlin.Boolean

    public open override fun isEmpty(): kotlin.Boolean

    public abstract fun lessThanOrEquals(a: T, b: T): kotlin.Boolean
}

public interface ClosedRange<T : kotlin.Comparable<T>> {
    public abstract val endInclusive: T { get; }

    public abstract val start: T { get; }

    public open operator fun contains(value: T): kotlin.Boolean

    public open fun isEmpty(): kotlin.Boolean
}

public open class IntProgression : kotlin.collections.Iterable<kotlin.Int> {
    public final val first: kotlin.Int { get; }

    public final val last: kotlin.Int { get; }

    public final val step: kotlin.Int { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.IntIterator

    public open override fun toString(): kotlin.String

    public companion object of IntProgression {
        public final fun fromClosedRange(rangeStart: kotlin.Int, rangeEnd: kotlin.Int, step: kotlin.Int): kotlin.ranges.IntProgression
    }
}

public final class IntRange : kotlin.ranges.IntProgression, kotlin.ranges.ClosedRange<kotlin.Int>, kotlin.ranges.OpenEndRange<kotlin.Int> {
    public constructor IntRange(start: kotlin.Int, endInclusive: kotlin.Int)

    @kotlin.Deprecated(message = "Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @kotlin.SinceKotlin(version = "1.9")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public open override val endExclusive: kotlin.Int { get; }

    public open override val endInclusive: kotlin.Int { get; }

    public open override val start: kotlin.Int { get; }

    public open override operator fun contains(value: kotlin.Int): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of IntRange {
        public final val EMPTY: kotlin.ranges.IntRange { get; }
    }
}

public open class LongProgression : kotlin.collections.Iterable<kotlin.Long> {
    public final val first: kotlin.Long { get; }

    public final val last: kotlin.Long { get; }

    public final val step: kotlin.Long { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.LongIterator

    public open override fun toString(): kotlin.String

    public companion object of LongProgression {
        public final fun fromClosedRange(rangeStart: kotlin.Long, rangeEnd: kotlin.Long, step: kotlin.Long): kotlin.ranges.LongProgression
    }
}

public final class LongRange : kotlin.ranges.LongProgression, kotlin.ranges.ClosedRange<kotlin.Long>, kotlin.ranges.OpenEndRange<kotlin.Long> {
    public constructor LongRange(start: kotlin.Long, endInclusive: kotlin.Long)

    @kotlin.Deprecated(message = "Can throw an exception when it's impossible to represent the value with Long type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @kotlin.SinceKotlin(version = "1.9")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public open override val endExclusive: kotlin.Long { get; }

    public open override val endInclusive: kotlin.Long { get; }

    public open override val start: kotlin.Long { get; }

    public open override operator fun contains(value: kotlin.Long): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of LongRange {
        public final val EMPTY: kotlin.ranges.LongRange { get; }
    }
}

@kotlin.SinceKotlin(version = "1.9")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public interface OpenEndRange<T : kotlin.Comparable<T>> {
    public abstract val endExclusive: T { get; }

    public abstract val start: T { get; }

    public open operator fun contains(value: T): kotlin.Boolean

    public open fun isEmpty(): kotlin.Boolean
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public open class UIntProgression : kotlin.collections.Iterable<kotlin.UInt> {
    public final val first: kotlin.UInt { get; }

    public final val last: kotlin.UInt { get; }

    public final val step: kotlin.Int { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open fun isEmpty(): kotlin.Boolean

    public final override operator fun iterator(): kotlin.collections.Iterator<kotlin.UInt>

    public open override fun toString(): kotlin.String

    public companion object of UIntProgression {
        public final fun fromClosedRange(rangeStart: kotlin.UInt, rangeEnd: kotlin.UInt, step: kotlin.Int): kotlin.ranges.UIntProgression
    }
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public final class UIntRange : kotlin.ranges.UIntProgression, kotlin.ranges.ClosedRange<kotlin.UInt>, kotlin.ranges.OpenEndRange<kotlin.UInt> {
    public constructor UIntRange(start: kotlin.UInt, endInclusive: kotlin.UInt)

    @kotlin.Deprecated(message = "Can throw an exception when it's impossible to represent the value with UInt type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @kotlin.SinceKotlin(version = "1.9")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public open override val endExclusive: kotlin.UInt { get; }

    public open override val endInclusive: kotlin.UInt { get; }

    public open override val start: kotlin.UInt { get; }

    public open override operator fun contains(value: kotlin.UInt): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of UIntRange {
        public final val EMPTY: kotlin.ranges.UIntRange { get; }
    }
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public open class ULongProgression : kotlin.collections.Iterable<kotlin.ULong> {
    public final val first: kotlin.ULong { get; }

    public final val last: kotlin.ULong { get; }

    public final val step: kotlin.Long { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open fun isEmpty(): kotlin.Boolean

    public final override operator fun iterator(): kotlin.collections.Iterator<kotlin.ULong>

    public open override fun toString(): kotlin.String

    public companion object of ULongProgression {
        public final fun fromClosedRange(rangeStart: kotlin.ULong, rangeEnd: kotlin.ULong, step: kotlin.Long): kotlin.ranges.ULongProgression
    }
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public final class ULongRange : kotlin.ranges.ULongProgression, kotlin.ranges.ClosedRange<kotlin.ULong>, kotlin.ranges.OpenEndRange<kotlin.ULong> {
    public constructor ULongRange(start: kotlin.ULong, endInclusive: kotlin.ULong)

    @kotlin.Deprecated(message = "Can throw an exception when it's impossible to represent the value with ULong type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @kotlin.SinceKotlin(version = "1.9")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public open override val endExclusive: kotlin.ULong { get; }

    public open override val endInclusive: kotlin.ULong { get; }

    public open override val start: kotlin.ULong { get; }

    public open override operator fun contains(value: kotlin.ULong): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of ULongRange {
        public final val EMPTY: kotlin.ranges.ULongRange { get; }
    }
}