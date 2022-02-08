@kotlin.internal.InlineOnly
public inline fun <T> compareBy(crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>

public fun <T> compareBy(vararg selectors: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T, K> compareBy(comparator: kotlin.Comparator<in K>, crossinline selector: (T) -> K): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T> compareByDescending(crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T, K> compareByDescending(comparator: kotlin.Comparator<in K>, crossinline selector: (T) -> K): kotlin.Comparator<T>

public fun <T : kotlin.Comparable<*>> compareValues(a: T?, b: T?): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun <T> compareValuesBy(a: T, b: T, selector: (T) -> kotlin.Comparable<*>?): kotlin.Int

public fun <T> compareValuesBy(a: T, b: T, vararg selectors: (T) -> kotlin.Comparable<*>?): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun <T, K> compareValuesBy(a: T, b: T, comparator: kotlin.Comparator<in K>, selector: (T) -> K): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
public fun <T : kotlin.Comparable<T>> maxOf(a: T, b: T): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T : kotlin.Comparable<T>> maxOf(a: T, b: T, c: T): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T> maxOf(a: T, b: T, c: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T> maxOf(a: T, b: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> maxOf(a: T, vararg other: T): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T> maxOf(a: T, vararg other: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Byte, b: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Byte, b: kotlin.Byte, c: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Byte, vararg other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Double, b: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Double, b: kotlin.Double, c: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Double, vararg other: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Float, b: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Float, b: kotlin.Float, c: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Float, vararg other: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Int, b: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Int, b: kotlin.Int, c: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Int, vararg other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
public inline fun maxOf(a: kotlin.Long, b: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Long, b: kotlin.Long, c: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Long, vararg other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Short, b: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.Short, b: kotlin.Short, c: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.4")
public fun maxOf(a: kotlin.Short, vararg other: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun maxOf(a: kotlin.UByte, b: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.UByte, b: kotlin.UByte, c: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun maxOf(a: kotlin.UByte, vararg other: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun maxOf(a: kotlin.UInt, b: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.UInt, b: kotlin.UInt, c: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun maxOf(a: kotlin.UInt, vararg other: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun maxOf(a: kotlin.ULong, b: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.ULong, b: kotlin.ULong, c: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun maxOf(a: kotlin.ULong, vararg other: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun maxOf(a: kotlin.UShort, b: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun maxOf(a: kotlin.UShort, b: kotlin.UShort, c: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun maxOf(a: kotlin.UShort, vararg other: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.1")
public fun <T : kotlin.Comparable<T>> minOf(a: T, b: T): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T : kotlin.Comparable<T>> minOf(a: T, b: T, c: T): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T> minOf(a: T, b: T, c: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.1")
public fun <T> minOf(a: T, b: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> minOf(a: T, vararg other: T): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T> minOf(a: T, vararg other: T, comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Byte, b: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Byte, b: kotlin.Byte, c: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Byte, vararg other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Double, b: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Double, b: kotlin.Double, c: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Double, vararg other: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Float, b: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Float, b: kotlin.Float, c: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Float, vararg other: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Int, b: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Int, b: kotlin.Int, c: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Int, vararg other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
public inline fun minOf(a: kotlin.Long, b: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Long, b: kotlin.Long, c: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Long, vararg other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Short, b: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.Short, b: kotlin.Short, c: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.4")
public fun minOf(a: kotlin.Short, vararg other: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun minOf(a: kotlin.UByte, b: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.UByte, b: kotlin.UByte, c: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun minOf(a: kotlin.UByte, vararg other: kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun minOf(a: kotlin.UInt, b: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.UInt, b: kotlin.UInt, c: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun minOf(a: kotlin.UInt, vararg other: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun minOf(a: kotlin.ULong, b: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.ULong, b: kotlin.ULong, c: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun minOf(a: kotlin.ULong, vararg other: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun minOf(a: kotlin.UShort, b: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun minOf(a: kotlin.UShort, b: kotlin.UShort, c: kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun minOf(a: kotlin.UShort, vararg other: kotlin.UShort): kotlin.UShort

public fun <T : kotlin.Comparable<T>> naturalOrder(): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Comparable<T>> nullsFirst(): kotlin.Comparator<T?>

public fun <T : kotlin.Any> nullsFirst(comparator: kotlin.Comparator<in T>): kotlin.Comparator<T?>

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Comparable<T>> nullsLast(): kotlin.Comparator<T?>

public fun <T : kotlin.Any> nullsLast(comparator: kotlin.Comparator<in T>): kotlin.Comparator<T?>

public fun <T : kotlin.Comparable<T>> reverseOrder(): kotlin.Comparator<T>

public fun <T> kotlin.Comparator<T>.reversed(): kotlin.Comparator<T>

public infix fun <T> kotlin.Comparator<T>.then(comparator: kotlin.Comparator<in T>): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Comparator<T>.thenBy(crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T, K> kotlin.Comparator<T>.thenBy(comparator: kotlin.Comparator<in K>, crossinline selector: (T) -> K): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Comparator<T>.thenByDescending(crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T, K> kotlin.Comparator<T>.thenByDescending(comparator: kotlin.Comparator<in K>, crossinline selector: (T) -> K): kotlin.Comparator<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Comparator<T>.thenComparator(crossinline comparison: (a: T, b: T) -> kotlin.Int): kotlin.Comparator<T>

public infix fun <T> kotlin.Comparator<T>.thenDescending(comparator: kotlin.Comparator<in T>): kotlin.Comparator<T>