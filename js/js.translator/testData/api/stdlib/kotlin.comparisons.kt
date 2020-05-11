package kotlin.comparisons

@kotlin.internal.InlineOnly public inline fun </*0*/ T> compareBy(/*0*/ crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>
public fun </*0*/ T> compareBy(/*0*/ vararg selectors: (T) -> kotlin.Comparable<*>? /*kotlin.Array<out (T) -> kotlin.Comparable<*>?>*/): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ K> compareBy(/*0*/ comparator: kotlin.Comparator<in K>, /*1*/ crossinline selector: (T) -> K): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> compareByDescending(/*0*/ crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ K> compareByDescending(/*0*/ comparator: kotlin.Comparator<in K>, /*1*/ crossinline selector: (T) -> K): kotlin.Comparator<T>
public fun </*0*/ T : kotlin.Comparable<*>> compareValues(/*0*/ a: T?, /*1*/ b: T?): kotlin.Int
@kotlin.internal.InlineOnly public inline fun </*0*/ T> compareValuesBy(/*0*/ a: T, /*1*/ b: T, /*2*/ selector: (T) -> kotlin.Comparable<*>?): kotlin.Int
public fun </*0*/ T> compareValuesBy(/*0*/ a: T, /*1*/ b: T, /*2*/ vararg selectors: (T) -> kotlin.Comparable<*>? /*kotlin.Array<out (T) -> kotlin.Comparable<*>?>*/): kotlin.Int
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ K> compareValuesBy(/*0*/ a: T, /*1*/ b: T, /*2*/ comparator: kotlin.Comparator<in K>, /*3*/ selector: (T) -> K): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T : kotlin.Comparable<T>> maxOf(/*0*/ a: T, /*1*/ b: T): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T : kotlin.Comparable<T>> maxOf(/*0*/ a: T, /*1*/ b: T, /*2*/ c: T): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T> maxOf(/*0*/ a: T, /*1*/ b: T, /*2*/ c: T, /*3*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T> maxOf(/*0*/ a: T, /*1*/ b: T, /*2*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T : kotlin.Comparable<T>> maxOf(/*0*/ a: T, /*1*/ vararg other: T /*kotlin.Array<out T>*/): T
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T> maxOf(/*0*/ a: T, /*1*/ vararg other: T /*kotlin.Array<out T>*/, /*2*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Byte, /*1*/ b: kotlin.Byte): kotlin.Byte
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Byte, /*1*/ b: kotlin.Byte, /*2*/ c: kotlin.Byte): kotlin.Byte
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Byte, /*1*/ vararg other: kotlin.Byte /*kotlin.ByteArray*/): kotlin.Byte
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double, /*2*/ c: kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Double, /*1*/ vararg other: kotlin.Double /*kotlin.DoubleArray*/): kotlin.Double
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Float, /*1*/ b: kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Float, /*1*/ b: kotlin.Float, /*2*/ c: kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Float, /*1*/ vararg other: kotlin.Float /*kotlin.FloatArray*/): kotlin.Float
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Int, /*1*/ b: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Int, /*1*/ b: kotlin.Int, /*2*/ c: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Int, /*1*/ vararg other: kotlin.Int /*kotlin.IntArray*/): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") public inline fun maxOf(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long, /*2*/ c: kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Long, /*1*/ vararg other: kotlin.Long /*kotlin.LongArray*/): kotlin.Long
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Short, /*1*/ b: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.Short, /*1*/ b: kotlin.Short, /*2*/ c: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.4") public fun maxOf(/*0*/ a: kotlin.Short, /*1*/ vararg other: kotlin.Short /*kotlin.ShortArray*/): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UByte, /*1*/ b: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.UByte, /*1*/ b: kotlin.UByte, /*2*/ c: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UByte, /*1*/ vararg other: kotlin.UByte /*kotlin.UByteArray*/): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UInt, /*1*/ b: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.UInt, /*1*/ b: kotlin.UInt, /*2*/ c: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UInt, /*1*/ vararg other: kotlin.UInt /*kotlin.UIntArray*/): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.ULong, /*1*/ b: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.ULong, /*1*/ b: kotlin.ULong, /*2*/ c: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.ULong, /*1*/ vararg other: kotlin.ULong /*kotlin.ULongArray*/): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UShort, /*1*/ b: kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun maxOf(/*0*/ a: kotlin.UShort, /*1*/ b: kotlin.UShort, /*2*/ c: kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun maxOf(/*0*/ a: kotlin.UShort, /*1*/ vararg other: kotlin.UShort /*kotlin.UShortArray*/): kotlin.UShort
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T : kotlin.Comparable<T>> minOf(/*0*/ a: T, /*1*/ b: T): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T : kotlin.Comparable<T>> minOf(/*0*/ a: T, /*1*/ b: T, /*2*/ c: T): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T> minOf(/*0*/ a: T, /*1*/ b: T, /*2*/ c: T, /*3*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T> minOf(/*0*/ a: T, /*1*/ b: T, /*2*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T : kotlin.Comparable<T>> minOf(/*0*/ a: T, /*1*/ vararg other: T /*kotlin.Array<out T>*/): T
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T> minOf(/*0*/ a: T, /*1*/ vararg other: T /*kotlin.Array<out T>*/, /*2*/ comparator: kotlin.Comparator<in T>): T
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Byte, /*1*/ b: kotlin.Byte): kotlin.Byte
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Byte, /*1*/ b: kotlin.Byte, /*2*/ c: kotlin.Byte): kotlin.Byte
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Byte, /*1*/ vararg other: kotlin.Byte /*kotlin.ByteArray*/): kotlin.Byte
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Double, /*1*/ b: kotlin.Double, /*2*/ c: kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Double, /*1*/ vararg other: kotlin.Double /*kotlin.DoubleArray*/): kotlin.Double
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Float, /*1*/ b: kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Float, /*1*/ b: kotlin.Float, /*2*/ c: kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Float, /*1*/ vararg other: kotlin.Float /*kotlin.FloatArray*/): kotlin.Float
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Int, /*1*/ b: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Int, /*1*/ b: kotlin.Int, /*2*/ c: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Int, /*1*/ vararg other: kotlin.Int /*kotlin.IntArray*/): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") public inline fun minOf(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Long, /*1*/ b: kotlin.Long, /*2*/ c: kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Long, /*1*/ vararg other: kotlin.Long /*kotlin.LongArray*/): kotlin.Long
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Short, /*1*/ b: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.Short, /*1*/ b: kotlin.Short, /*2*/ c: kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.4") public fun minOf(/*0*/ a: kotlin.Short, /*1*/ vararg other: kotlin.Short /*kotlin.ShortArray*/): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UByte, /*1*/ b: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.UByte, /*1*/ b: kotlin.UByte, /*2*/ c: kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UByte, /*1*/ vararg other: kotlin.UByte /*kotlin.UByteArray*/): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UInt, /*1*/ b: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.UInt, /*1*/ b: kotlin.UInt, /*2*/ c: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UInt, /*1*/ vararg other: kotlin.UInt /*kotlin.UIntArray*/): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.ULong, /*1*/ b: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.ULong, /*1*/ b: kotlin.ULong, /*2*/ c: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.ULong, /*1*/ vararg other: kotlin.ULong /*kotlin.ULongArray*/): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UShort, /*1*/ b: kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun minOf(/*0*/ a: kotlin.UShort, /*1*/ b: kotlin.UShort, /*2*/ c: kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun minOf(/*0*/ a: kotlin.UShort, /*1*/ vararg other: kotlin.UShort /*kotlin.UShortArray*/): kotlin.UShort
public fun </*0*/ T : kotlin.Comparable<T>> naturalOrder(): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Comparable<T>> nullsFirst(): kotlin.Comparator<T?>
public fun </*0*/ T : kotlin.Any> nullsFirst(/*0*/ comparator: kotlin.Comparator<in T>): kotlin.Comparator<T?>
@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Comparable<T>> nullsLast(): kotlin.Comparator<T?>
public fun </*0*/ T : kotlin.Any> nullsLast(/*0*/ comparator: kotlin.Comparator<in T>): kotlin.Comparator<T?>
public fun </*0*/ T : kotlin.Comparable<T>> reverseOrder(): kotlin.Comparator<T>
public fun </*0*/ T> kotlin.Comparator<T>.reversed(): kotlin.Comparator<T>
public infix fun </*0*/ T> kotlin.Comparator<T>.then(/*0*/ comparator: kotlin.Comparator<in T>): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Comparator<T>.thenBy(/*0*/ crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ K> kotlin.Comparator<T>.thenBy(/*0*/ comparator: kotlin.Comparator<in K>, /*1*/ crossinline selector: (T) -> K): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Comparator<T>.thenByDescending(/*0*/ crossinline selector: (T) -> kotlin.Comparable<*>?): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ K> kotlin.Comparator<T>.thenByDescending(/*0*/ comparator: kotlin.Comparator<in K>, /*1*/ crossinline selector: (T) -> K): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Comparator<T>.thenComparator(/*0*/ crossinline comparison: (a: T, b: T) -> kotlin.Int): kotlin.Comparator<T>
public infix fun </*0*/ T> kotlin.Comparator<T>.thenDescending(/*0*/ comparator: kotlin.Comparator<in T>): kotlin.Comparator<T>