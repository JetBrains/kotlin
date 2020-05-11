package kotlin

@kotlin.SinceKotlin(version = "1.2") @kotlin.internal.InlineOnly public val kotlin.reflect.KProperty0<*>.isInitialized: kotlin.Boolean
    public inline fun kotlin.reflect.KProperty0<*>.<get-isInitialized>(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") public val kotlin.Throwable.suppressedExceptions: kotlin.collections.List<kotlin.Throwable>
    public fun kotlin.Throwable.<get-suppressedExceptions>(): kotlin.collections.List<kotlin.Throwable>
public inline fun </*0*/ T> Comparator(/*0*/ crossinline comparison: (a: T, b: T) -> kotlin.Int): kotlin.Comparator<T>
@kotlin.internal.InlineOnly public inline fun TODO(): kotlin.Nothing
@kotlin.internal.InlineOnly public inline fun TODO(/*0*/ reason: kotlin.String): kotlin.Nothing
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun UByteArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.UByte): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun UIntArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.UInt): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun ULongArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.ULong): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun UShortArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.UShort): kotlin.UShortArray
@kotlin.js.library public fun </*0*/ T> arrayOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.Array<T>
public inline fun </*0*/ reified @kotlin.internal.PureReifiable T> arrayOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.Array<T>
public fun </*0*/ reified @kotlin.internal.PureReifiable T> arrayOfNulls(/*0*/ size: kotlin.Int): kotlin.Array<T?>
@kotlin.js.library public fun booleanArrayOf(/*0*/ vararg elements: kotlin.Boolean /*kotlin.BooleanArray*/): kotlin.BooleanArray
public fun booleanArrayOf(/*0*/ vararg elements: kotlin.Boolean /*kotlin.BooleanArray*/): kotlin.BooleanArray
@kotlin.js.library public fun byteArrayOf(/*0*/ vararg elements: kotlin.Byte /*kotlin.ByteArray*/): kotlin.ByteArray
public fun byteArrayOf(/*0*/ vararg elements: kotlin.Byte /*kotlin.ByteArray*/): kotlin.ByteArray
@kotlin.js.library public fun charArrayOf(/*0*/ vararg elements: kotlin.Char /*kotlin.CharArray*/): kotlin.CharArray
public fun charArrayOf(/*0*/ vararg elements: kotlin.Char /*kotlin.CharArray*/): kotlin.CharArray
@kotlin.internal.InlineOnly public inline fun check(/*0*/ value: kotlin.Boolean): kotlin.Unit
    Returns(WILDCARD) -> value

@kotlin.internal.InlineOnly public inline fun check(/*0*/ value: kotlin.Boolean, /*1*/ lazyMessage: () -> kotlin.Any): kotlin.Unit
    Returns(WILDCARD) -> value

@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Any> checkNotNull(/*0*/ value: T?): T
    Returns(WILDCARD) -> value != null

@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Any> checkNotNull(/*0*/ value: T?, /*1*/ lazyMessage: () -> kotlin.Any): T
    Returns(WILDCARD) -> value != null

@kotlin.js.library public fun doubleArrayOf(/*0*/ vararg elements: kotlin.Double /*kotlin.DoubleArray*/): kotlin.DoubleArray
public fun doubleArrayOf(/*0*/ vararg elements: kotlin.Double /*kotlin.DoubleArray*/): kotlin.DoubleArray
public inline fun </*0*/ T> emptyArray(): kotlin.Array<T>
public inline fun </*0*/ reified @kotlin.internal.PureReifiable T> emptyArray(): kotlin.Array<T>
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ reified T : kotlin.Enum<T>> enumValueOf(/*0*/ name: kotlin.String): T
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ reified T : kotlin.Enum<T>> enumValues(): kotlin.Array<T>
@kotlin.internal.InlineOnly public inline fun error(/*0*/ message: kotlin.Any): kotlin.Nothing
@kotlin.js.library public fun floatArrayOf(/*0*/ vararg elements: kotlin.Float /*kotlin.FloatArray*/): kotlin.FloatArray
public fun floatArrayOf(/*0*/ vararg elements: kotlin.Float /*kotlin.FloatArray*/): kotlin.FloatArray
@kotlin.js.library public fun intArrayOf(/*0*/ vararg elements: kotlin.Int /*kotlin.IntArray*/): kotlin.IntArray
public fun intArrayOf(/*0*/ vararg elements: kotlin.Int /*kotlin.IntArray*/): kotlin.IntArray
public fun </*0*/ T> lazy(/*0*/ initializer: () -> T): kotlin.Lazy<T>
public fun </*0*/ T> lazy(/*0*/ lock: kotlin.Any?, /*1*/ initializer: () -> T): kotlin.Lazy<T>
public fun </*0*/ T> lazy(/*0*/ mode: kotlin.LazyThreadSafetyMode, /*1*/ initializer: () -> T): kotlin.Lazy<T>
public fun </*0*/ T> lazyOf(/*0*/ value: T): kotlin.Lazy<T>
@kotlin.js.library public fun longArrayOf(/*0*/ vararg elements: kotlin.Long /*kotlin.LongArray*/): kotlin.LongArray
public fun longArrayOf(/*0*/ vararg elements: kotlin.Long /*kotlin.LongArray*/): kotlin.LongArray
@kotlin.internal.InlineOnly public inline fun repeat(/*0*/ times: kotlin.Int, /*1*/ action: (kotlin.Int) -> kotlin.Unit): kotlin.Unit
    CallsInPlace(action, UNKNOWN)

@kotlin.internal.InlineOnly public inline fun require(/*0*/ value: kotlin.Boolean): kotlin.Unit
    Returns(WILDCARD) -> value

@kotlin.internal.InlineOnly public inline fun require(/*0*/ value: kotlin.Boolean, /*1*/ lazyMessage: () -> kotlin.Any): kotlin.Unit
    Returns(WILDCARD) -> value

@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Any> requireNotNull(/*0*/ value: T?): T
    Returns(WILDCARD) -> value != null

@kotlin.internal.InlineOnly public inline fun </*0*/ T : kotlin.Any> requireNotNull(/*0*/ value: T?, /*1*/ lazyMessage: () -> kotlin.Any): T
    Returns(WILDCARD) -> value != null

@kotlin.internal.InlineOnly public inline fun </*0*/ R> run(/*0*/ block: () -> R): R
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R> runCatching(/*0*/ block: () -> R): kotlin.Result<R>
@kotlin.js.library public fun shortArrayOf(/*0*/ vararg elements: kotlin.Short /*kotlin.ShortArray*/): kotlin.ShortArray
public fun shortArrayOf(/*0*/ vararg elements: kotlin.Short /*kotlin.ShortArray*/): kotlin.ShortArray
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.2") public inline fun </*0*/ R> suspend(/*0*/ noinline block: suspend () -> R): suspend () -> R
@kotlin.internal.InlineOnly public inline fun </*0*/ R> synchronized(/*0*/ lock: kotlin.Any, /*1*/ block: () -> R): R
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun ubyteArrayOf(/*0*/ vararg elements: kotlin.UByte /*kotlin.UByteArray*/): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun uintArrayOf(/*0*/ vararg elements: kotlin.UInt /*kotlin.UIntArray*/): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun ulongArrayOf(/*0*/ vararg elements: kotlin.ULong /*kotlin.ULongArray*/): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun ushortArrayOf(/*0*/ vararg elements: kotlin.UShort /*kotlin.UShortArray*/): kotlin.UShortArray
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> with(/*0*/ receiver: T, /*1*/ block: T.() -> R): R
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.4") public fun kotlin.Throwable.addSuppressed(/*0*/ exception: kotlin.Throwable): kotlin.Unit
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T> T.also(/*0*/ block: (T) -> kotlin.Unit): T
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.internal.InlineOnly public inline fun </*0*/ T> T.apply(/*0*/ block: T.() -> kotlin.Unit): T
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Byte.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Int.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Long.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Short.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.countLeadingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Byte.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Int.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Long.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Short.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.countOneBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Byte.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Int.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Long.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Short.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.countTrailingZeroBits(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.countTrailingZeroBits(): kotlin.Int
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T> kotlin.Result<T>.fold(/*0*/ onSuccess: (value: T) -> R, /*1*/ onFailure: (exception: kotlin.Throwable) -> R): R
    CallsInPlace(onSuccess, AT_MOST_ONCE)
    CallsInPlace(onFailure, AT_MOST_ONCE)

@kotlin.SinceKotlin(version = "1.2") @kotlin.internal.InlineOnly public inline fun kotlin.Double.Companion.fromBits(/*0*/ bits: kotlin.Long): kotlin.Double
@kotlin.SinceKotlin(version = "1.2") @kotlin.internal.InlineOnly public inline fun kotlin.Float.Companion.fromBits(/*0*/ bits: kotlin.Int): kotlin.Float
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T : R> kotlin.Result<T>.getOrDefault(/*0*/ defaultValue: R): R
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T : R> kotlin.Result<T>.getOrElse(/*0*/ onFailure: (exception: kotlin.Throwable) -> R): R
    CallsInPlace(onFailure, AT_MOST_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ T> kotlin.Result<T>.getOrThrow(): T
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Lazy<T>.getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): T
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline operator fun </*0*/ V> kotlin.reflect.KProperty0<V>.getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): V
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline operator fun </*0*/ T, /*1*/ V> kotlin.reflect.KProperty1<T, V>.getValue(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>): V
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.Any?.hashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi public operator fun </*0*/ T, /*1*/ R> kotlin.DeepRecursiveFunction<T, R>.invoke(/*0*/ value: T): R
public fun kotlin.Double.isFinite(): kotlin.Boolean
public fun kotlin.Float.isFinite(): kotlin.Boolean
public fun kotlin.Double.isInfinite(): kotlin.Boolean
public fun kotlin.Float.isInfinite(): kotlin.Boolean
public fun kotlin.Double.isNaN(): kotlin.Boolean
public fun kotlin.Float.isNaN(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> T.let(/*0*/ block: (T) -> R): R
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T> kotlin.Result<T>.map(/*0*/ transform: (value: T) -> R): kotlin.Result<R>
    CallsInPlace(transform, AT_MOST_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T> kotlin.Result<T>.mapCatching(/*0*/ transform: (value: T) -> R): kotlin.Result<R>
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ T> kotlin.Result<T>.onFailure(/*0*/ action: (exception: kotlin.Throwable) -> kotlin.Unit): kotlin.Result<T>
    CallsInPlace(action, AT_MOST_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ T> kotlin.Result<T>.onSuccess(/*0*/ action: (value: T) -> kotlin.Unit): kotlin.Result<T>
    CallsInPlace(action, AT_MOST_ONCE)

public operator fun kotlin.String?.plus(/*0*/ other: kotlin.Any?): kotlin.String
@kotlin.SinceKotlin(version = "1.4") public fun kotlin.Throwable.printStackTrace(): kotlin.Unit
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T : R> kotlin.Result<T>.recover(/*0*/ transform: (exception: kotlin.Throwable) -> R): kotlin.Result<R>
    CallsInPlace(transform, AT_MOST_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ R, /*1*/ T : R> kotlin.Result<T>.recoverCatching(/*0*/ transform: (exception: kotlin.Throwable) -> R): kotlin.Result<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Byte.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.Byte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Int.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Long.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Short.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.rotateLeft(/*0*/ bitCount: kotlin.Int): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Byte.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.Byte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Int.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.Long.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.Short.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.rotateRight(/*0*/ bitCount: kotlin.Int): kotlin.UShort
@kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> T.run(/*0*/ block: T.() -> R): R
    CallsInPlace(block, EXACTLY_ONCE)

@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ T, /*1*/ R> T.runCatching(/*0*/ block: T.() -> R): kotlin.Result<R>
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline operator fun </*0*/ V> kotlin.reflect.KMutableProperty0<V>.setValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>, /*2*/ value: V): kotlin.Unit
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline operator fun </*0*/ T, /*1*/ V> kotlin.reflect.KMutableProperty1<T, V>.setValue(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>, /*2*/ value: V): kotlin.Unit
@kotlin.SinceKotlin(version = "1.4") public fun kotlin.Throwable.stackTraceToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Byte.takeHighestOneBit(): kotlin.Byte
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Int.takeHighestOneBit(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Long.takeHighestOneBit(): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Short.takeHighestOneBit(): kotlin.Short
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.takeHighestOneBit(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.takeHighestOneBit(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.takeHighestOneBit(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.takeHighestOneBit(): kotlin.UShort
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T> T.takeIf(/*0*/ predicate: (T) -> kotlin.Boolean): T?
    CallsInPlace(predicate, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Byte.takeLowestOneBit(): kotlin.Byte
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Int.takeLowestOneBit(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun kotlin.Long.takeLowestOneBit(): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.internal.InlineOnly public inline fun kotlin.Short.takeLowestOneBit(): kotlin.Short
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByte.takeLowestOneBit(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UInt.takeLowestOneBit(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULong.takeLowestOneBit(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShort.takeLowestOneBit(): kotlin.UShort
@kotlin.internal.InlineOnly @kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T> T.takeUnless(/*0*/ predicate: (T) -> kotlin.Boolean): T?
    CallsInPlace(predicate, EXACTLY_ONCE)

public infix fun </*0*/ A, /*1*/ B> A.to(/*0*/ that: B): kotlin.Pair<A, B>
@kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "doubleToBits") public fun kotlin.Double.toBits(): kotlin.Long
@kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "floatToBits") public fun kotlin.Float.toBits(): kotlin.Int
public fun </*0*/ T> kotlin.Pair<T, T>.toList(): kotlin.collections.List<T>
public fun </*0*/ T> kotlin.Triple<T, T, T>.toList(): kotlin.collections.List<T>
@kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "doubleToRawBits") public fun kotlin.Double.toRawBits(): kotlin.Long
@kotlin.SinceKotlin(version = "1.2") @kotlin.js.library(name = "floatToRawBits") public fun kotlin.Float.toRawBits(): kotlin.Int
public fun kotlin.Any?.toString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Byte.toUByte(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Int.toUByte(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Long.toUByte(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Short.toUByte(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Byte.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Double.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Float.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Int.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Long.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Short.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Byte.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Double.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Float.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Int.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Long.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Short.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Byte.toUShort(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Int.toUShort(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Long.toUShort(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.Short.toUShort(): kotlin.UShort

public interface Annotation {
}

public interface Annotation {
}

public open class Any {
    /*primary*/ public constructor Any()
    public open operator fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open fun hashCode(): kotlin.Int
    public open fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") public open class ArithmeticException : kotlin.RuntimeException {
    public constructor ArithmeticException()
    /*primary*/ public constructor ArithmeticException(/*0*/ message: kotlin.String?)
}

public final class Array</*0*/ T> {
    public constructor Array</*0*/ T>(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> T)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): T
    public final operator fun iterator(): kotlin.collections.Iterator<T>
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: T): kotlin.Unit
}

public open class AssertionError : kotlin.Error {
    public constructor AssertionError()
    public constructor AssertionError(/*0*/ message: kotlin.Any?)
    public constructor AssertionError(/*0*/ message: kotlin.String?)
    /*primary*/ @kotlin.SinceKotlin(version = "1.4") public constructor AssertionError(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
}

public final class Boolean : kotlin.Comparable<kotlin.Boolean> {
    public final infix fun and(/*0*/ other: kotlin.Boolean): kotlin.Boolean
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Boolean): kotlin.Int
    public final operator fun not(): kotlin.Boolean
    public final infix fun or(/*0*/ other: kotlin.Boolean): kotlin.Boolean
    public final infix fun xor(/*0*/ other: kotlin.Boolean): kotlin.Boolean

    @kotlin.SinceKotlin(version = "1.3") public companion object Companion {
    }
}

public final class BooleanArray {
    /*primary*/ public constructor BooleanArray(/*0*/ size: kotlin.Int)
    public constructor BooleanArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Boolean)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Boolean
    public final operator fun iterator(): kotlin.collections.BooleanIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Boolean): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.3") @kotlin.experimental.ExperimentalTypeInference public final annotation class BuilderInference : kotlin.Annotation {
    /*primary*/ public constructor BuilderInference()
}

public final class Byte : kotlin.Number, kotlin.Comparable<kotlin.Byte> {
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Byte
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun inc(): kotlin.Byte
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun rangeTo(/*0*/ other: kotlin.Byte): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Int): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Long): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Short): kotlin.ranges.IntRange
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Int
    public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    public open override /*1*/ fun toShort(): kotlin.Short
    public final operator fun unaryMinus(): kotlin.Int
    public final operator fun unaryPlus(): kotlin.Int

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Byte = 127.toByte()
            public final fun <get-MAX_VALUE>(): kotlin.Byte
        public const final val MIN_VALUE: kotlin.Byte = -128.toByte()
            public final fun <get-MIN_VALUE>(): kotlin.Byte
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BITS: kotlin.Int = 8
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BYTES: kotlin.Int = 1
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class ByteArray {
    /*primary*/ public constructor ByteArray(/*0*/ size: kotlin.Int)
    public constructor ByteArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Byte)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Byte
    public final operator fun iterator(): kotlin.collections.ByteIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit
}

public final class Char : kotlin.Comparable<kotlin.Char> {
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Char): kotlin.Int
    public final operator fun dec(): kotlin.Char
    public final operator fun inc(): kotlin.Char
    public final operator fun minus(/*0*/ other: kotlin.Char): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Char
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Char
    public final operator fun rangeTo(/*0*/ other: kotlin.Char): kotlin.ranges.CharRange
    public final fun toByte(): kotlin.Byte
    public final fun toChar(): kotlin.Char
    public final fun toDouble(): kotlin.Double
    public final fun toFloat(): kotlin.Float
    public final fun toInt(): kotlin.Int
    public final fun toLong(): kotlin.Long
    public final fun toShort(): kotlin.Short

    public companion object Companion {
        public const final val MAX_HIGH_SURROGATE: kotlin.Char = \uDBFF ('?')
            public final fun <get-MAX_HIGH_SURROGATE>(): kotlin.Char
        public const final val MAX_LOW_SURROGATE: kotlin.Char = \uDFFF ('?')
            public final fun <get-MAX_LOW_SURROGATE>(): kotlin.Char
        public const final val MAX_SURROGATE: kotlin.Char = \uDFFF ('?')
            public final fun <get-MAX_SURROGATE>(): kotlin.Char
        @kotlin.SinceKotlin(version = "1.3") public const final val MAX_VALUE: kotlin.Char = \uFFFF ('?')
            public final fun <get-MAX_VALUE>(): kotlin.Char
        public const final val MIN_HIGH_SURROGATE: kotlin.Char = \uD800 ('?')
            public final fun <get-MIN_HIGH_SURROGATE>(): kotlin.Char
        public const final val MIN_LOW_SURROGATE: kotlin.Char = \uDC00 ('?')
            public final fun <get-MIN_LOW_SURROGATE>(): kotlin.Char
        public const final val MIN_SURROGATE: kotlin.Char = \uD800 ('?')
            public final fun <get-MIN_SURROGATE>(): kotlin.Char
        @kotlin.SinceKotlin(version = "1.3") public const final val MIN_VALUE: kotlin.Char = \u0000 ('?')
            public final fun <get-MIN_VALUE>(): kotlin.Char
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BITS: kotlin.Int = 16
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BYTES: kotlin.Int = 2
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class CharArray {
    /*primary*/ public constructor CharArray(/*0*/ size: kotlin.Int)
    public constructor CharArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Char)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Char
    public final operator fun iterator(): kotlin.collections.CharIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Char): kotlin.Unit
}

public interface CharSequence {
    public abstract val length: kotlin.Int
        public abstract fun <get-length>(): kotlin.Int
    public abstract operator fun get(/*0*/ index: kotlin.Int): kotlin.Char
    public abstract fun subSequence(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
}

public interface CharSequence {
    public abstract val length: kotlin.Int
        public abstract fun <get-length>(): kotlin.Int
    public abstract operator fun get(/*0*/ index: kotlin.Int): kotlin.Char
    public abstract fun subSequence(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
}

public open class ClassCastException : kotlin.RuntimeException {
    public constructor ClassCastException()
    /*primary*/ public constructor ClassCastException(/*0*/ message: kotlin.String?)
}

public interface Comparable</*0*/ in T> {
    public abstract operator fun compareTo(/*0*/ other: T): kotlin.Int
}

public interface Comparator</*0*/ T> {
    @kotlin.js.JsName(name = "compare") public abstract fun compare(/*0*/ a: T, /*1*/ b: T): kotlin.Int
}

public open class ConcurrentModificationException : kotlin.RuntimeException {
    public constructor ConcurrentModificationException()
    public constructor ConcurrentModificationException(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor ConcurrentModificationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor ConcurrentModificationException(/*0*/ cause: kotlin.Throwable?)
}

@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi public final class DeepRecursiveFunction</*0*/ T, /*1*/ R> {
    /*primary*/ public constructor DeepRecursiveFunction</*0*/ T, /*1*/ R>(/*0*/ block: suspend kotlin.DeepRecursiveScope<T, R>.(T) -> R)
}

@kotlin.coroutines.RestrictsSuspension @kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi public sealed class DeepRecursiveScope</*0*/ T, /*1*/ R> {
    public abstract suspend fun callRecursive(/*0*/ value: T): R
    public abstract suspend fun </*0*/ U, /*1*/ S> kotlin.DeepRecursiveFunction<U, S>.callRecursive(/*0*/ value: U): S
    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "'invoke' should not be called from DeepRecursiveScope. Use 'callRecursive' to do recursion in the heap instead of the call stack.", replaceWith = kotlin.ReplaceWith(expression = "this.callRecursive(value)", imports = {})) public final operator fun kotlin.DeepRecursiveFunction<*, *>.invoke(/*0*/ value: kotlin.Any?): kotlin.Nothing
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.MustBeDocumented public final annotation class Deprecated : kotlin.Annotation {
    /*primary*/ public constructor Deprecated(/*0*/ message: kotlin.String, /*1*/ replaceWith: kotlin.ReplaceWith = ..., /*2*/ level: kotlin.DeprecationLevel = ...)
    public final val level: kotlin.DeprecationLevel
        public final fun <get-level>(): kotlin.DeprecationLevel
    public final val message: kotlin.String
        public final fun <get-message>(): kotlin.String
    public final val replaceWith: kotlin.ReplaceWith
        public final fun <get-replaceWith>(): kotlin.ReplaceWith
}

public final enum class DeprecationLevel : kotlin.Enum<kotlin.DeprecationLevel> {
    enum entry WARNING

    enum entry ERROR

    enum entry HIDDEN

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.DeprecationLevel
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.DeprecationLevel>
}

public final class Double : kotlin.Number, kotlin.Comparable<kotlin.Double> {
    public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Double
    public final operator fun inc(): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Double
    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toByte()", imports = {})) public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toShort()", imports = {})) public open override /*1*/ fun toShort(): kotlin.Short
    public final operator fun unaryMinus(): kotlin.Double
    public final operator fun unaryPlus(): kotlin.Double

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Double = 1.7976931348623157E308.toDouble()
            public final fun <get-MAX_VALUE>(): kotlin.Double
        public const final val MIN_VALUE: kotlin.Double = 4.9E-324.toDouble()
            public final fun <get-MIN_VALUE>(): kotlin.Double
        public const final val NEGATIVE_INFINITY: kotlin.Double = -Infinity.toDouble()
            public final fun <get-NEGATIVE_INFINITY>(): kotlin.Double
        public const final val NaN: kotlin.Double = NaN.toDouble()
            public final fun <get-NaN>(): kotlin.Double
        public const final val POSITIVE_INFINITY: kotlin.Double = Infinity.toDouble()
            public final fun <get-POSITIVE_INFINITY>(): kotlin.Double
        @kotlin.SinceKotlin(version = "1.4") public const final val SIZE_BITS: kotlin.Int = 64
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.4") public const final val SIZE_BYTES: kotlin.Int = 8
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class DoubleArray {
    /*primary*/ public constructor DoubleArray(/*0*/ size: kotlin.Int)
    public constructor DoubleArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Double)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Double
    public final operator fun iterator(): kotlin.collections.DoubleIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Double): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.MustBeDocumented @kotlin.SinceKotlin(version = "1.1") public final annotation class DslMarker : kotlin.Annotation {
    /*primary*/ public constructor DslMarker()
}

public abstract class Enum</*0*/ E : kotlin.Enum<E>> : kotlin.Comparable<E> {
    /*primary*/ public constructor Enum</*0*/ E : kotlin.Enum<E>>(/*0*/ name: kotlin.String, /*1*/ ordinal: kotlin.Int)
    public final val name: kotlin.String
        public final fun <get-name>(): kotlin.String
    public final val ordinal: kotlin.Int
        public final fun <get-ordinal>(): kotlin.Int
    protected final fun clone(): kotlin.Any
    public final override /*1*/ fun compareTo(/*0*/ other: E): kotlin.Int
    public final override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final override /*1*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
    }
}

public open class Error : kotlin.Throwable {
    public constructor Error()
    public constructor Error(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor Error(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor Error(/*0*/ cause: kotlin.Throwable?)
}

public open class Exception : kotlin.Throwable {
    public constructor Exception()
    public constructor Exception(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor Exception(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor Exception(/*0*/ cause: kotlin.Throwable?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.2") @kotlin.Deprecated(message = "Please use RequiresOptIn instead.") public final annotation class Experimental : kotlin.Annotation {
    /*primary*/ public constructor Experimental(/*0*/ level: kotlin.Experimental.Level = ...)
    public final val level: kotlin.Experimental.Level
        public final fun <get-level>(): kotlin.Experimental.Level

    public final enum class Level : kotlin.Enum<kotlin.Experimental.Level> {
        enum entry WARNING

        enum entry ERROR

        // Static members
        public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.Experimental.Level
        public final /*synthesized*/ fun values(): kotlin.Array<kotlin.Experimental.Level>
    }
}

@kotlin.Experimental @kotlin.RequiresOptIn @kotlin.annotation.MustBeDocumented @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) public final annotation class ExperimentalMultiplatform : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalMultiplatform()
}

@kotlin.Experimental(level = Level.ERROR) @kotlin.RequiresOptIn(level = Level.ERROR) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.MustBeDocumented @kotlin.SinceKotlin(version = "1.3") public final annotation class ExperimentalStdlibApi : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalStdlibApi()
}

@kotlin.Experimental(level = Level.WARNING) @kotlin.RequiresOptIn(level = Level.WARNING) @kotlin.annotation.MustBeDocumented @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) public final annotation class ExperimentalUnsignedTypes : kotlin.Annotation {
    /*primary*/ public constructor ExperimentalUnsignedTypes()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE}) @kotlin.annotation.MustBeDocumented public final annotation class ExtensionFunctionType : kotlin.Annotation {
    /*primary*/ public constructor ExtensionFunctionType()
}

public final class Float : kotlin.Number, kotlin.Comparable<kotlin.Float> {
    public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Float
    public final operator fun inc(): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Float
    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toByte()", imports = {})) public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toShort()", imports = {})) public open override /*1*/ fun toShort(): kotlin.Short
    public final operator fun unaryMinus(): kotlin.Float
    public final operator fun unaryPlus(): kotlin.Float

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Float = 3.4028235E38.toFloat()
            public final fun <get-MAX_VALUE>(): kotlin.Float
        public const final val MIN_VALUE: kotlin.Float = 1.4E-45.toFloat()
            public final fun <get-MIN_VALUE>(): kotlin.Float
        public const final val NEGATIVE_INFINITY: kotlin.Float = -Infinity.toFloat()
            public final fun <get-NEGATIVE_INFINITY>(): kotlin.Float
        public const final val NaN: kotlin.Float = NaN.toFloat()
            public final fun <get-NaN>(): kotlin.Float
        public const final val POSITIVE_INFINITY: kotlin.Float = Infinity.toFloat()
            public final fun <get-POSITIVE_INFINITY>(): kotlin.Float
        @kotlin.SinceKotlin(version = "1.4") public const final val SIZE_BITS: kotlin.Int = 32
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.4") public const final val SIZE_BYTES: kotlin.Int = 4
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class FloatArray {
    /*primary*/ public constructor FloatArray(/*0*/ size: kotlin.Int)
    public constructor FloatArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Float)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Float
    public final operator fun iterator(): kotlin.collections.FloatIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Float): kotlin.Unit
}

public interface Function</*0*/ out R> {
}

public interface Function</*0*/ out R> {
}

public open class IllegalArgumentException : kotlin.RuntimeException {
    public constructor IllegalArgumentException()
    public constructor IllegalArgumentException(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor IllegalArgumentException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor IllegalArgumentException(/*0*/ cause: kotlin.Throwable?)
}

public open class IllegalStateException : kotlin.RuntimeException {
    public constructor IllegalStateException()
    public constructor IllegalStateException(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor IllegalStateException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor IllegalStateException(/*0*/ cause: kotlin.Throwable?)
}

public open class IndexOutOfBoundsException : kotlin.RuntimeException {
    public constructor IndexOutOfBoundsException()
    /*primary*/ public constructor IndexOutOfBoundsException(/*0*/ message: kotlin.String?)
}

public final class Int : kotlin.Number, kotlin.Comparable<kotlin.Int> {
    public final infix fun and(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun inc(): kotlin.Int
    public final fun inv(): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Int
    public final infix fun or(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun rangeTo(/*0*/ other: kotlin.Byte): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Int): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Long): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Short): kotlin.ranges.IntRange
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Int
    public final infix fun shl(/*0*/ bitCount: kotlin.Int): kotlin.Int
    public final infix fun shr(/*0*/ bitCount: kotlin.Int): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Int
    public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    public open override /*1*/ fun toShort(): kotlin.Short
    public final operator fun unaryMinus(): kotlin.Int
    public final operator fun unaryPlus(): kotlin.Int
    public final infix fun ushr(/*0*/ bitCount: kotlin.Int): kotlin.Int
    public final infix fun xor(/*0*/ other: kotlin.Int): kotlin.Int

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Int = 2147483647
            public final fun <get-MAX_VALUE>(): kotlin.Int
        public const final val MIN_VALUE: kotlin.Int = -2147483648
            public final fun <get-MIN_VALUE>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BITS: kotlin.Int = 32
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BYTES: kotlin.Int = 4
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class IntArray {
    /*primary*/ public constructor IntArray(/*0*/ size: kotlin.Int)
    public constructor IntArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Int)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Int
    public final operator fun iterator(): kotlin.collections.IntIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Int): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.1") public final class KotlinVersion : kotlin.Comparable<kotlin.KotlinVersion> {
    public constructor KotlinVersion(/*0*/ major: kotlin.Int, /*1*/ minor: kotlin.Int)
    /*primary*/ public constructor KotlinVersion(/*0*/ major: kotlin.Int, /*1*/ minor: kotlin.Int, /*2*/ patch: kotlin.Int)
    public final val major: kotlin.Int
        public final fun <get-major>(): kotlin.Int
    public final val minor: kotlin.Int
        public final fun <get-minor>(): kotlin.Int
    public final val patch: kotlin.Int
        public final fun <get-patch>(): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.KotlinVersion): kotlin.Int
    public open override /*1*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun hashCode(): kotlin.Int
    public final fun isAtLeast(/*0*/ major: kotlin.Int, /*1*/ minor: kotlin.Int): kotlin.Boolean
    public final fun isAtLeast(/*0*/ major: kotlin.Int, /*1*/ minor: kotlin.Int, /*2*/ patch: kotlin.Int): kotlin.Boolean
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final val CURRENT: kotlin.KotlinVersion
            public final fun <get-CURRENT>(): kotlin.KotlinVersion
        public const final val MAX_COMPONENT_VALUE: kotlin.Int = 255
            public final fun <get-MAX_COMPONENT_VALUE>(): kotlin.Int
    }
}

public interface Lazy</*0*/ out T> {
    public abstract val value: T
        public abstract fun <get-value>(): T
    public abstract fun isInitialized(): kotlin.Boolean
}

public final enum class LazyThreadSafetyMode : kotlin.Enum<kotlin.LazyThreadSafetyMode> {
    enum entry SYNCHRONIZED

    enum entry PUBLICATION

    enum entry NONE

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.LazyThreadSafetyMode
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.LazyThreadSafetyMode>
}

public final class Long : kotlin.Number, kotlin.Comparable<kotlin.Long> {
    public final infix fun and(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Long
    public final operator fun inc(): kotlin.Long
    public final fun inv(): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Long
    public final infix fun or(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Long
    public final operator fun rangeTo(/*0*/ other: kotlin.Byte): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Int): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Long): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Short): kotlin.ranges.LongRange
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Long
    public final infix fun shl(/*0*/ bitCount: kotlin.Int): kotlin.Long
    public final infix fun shr(/*0*/ bitCount: kotlin.Int): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Long
    public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    public open override /*1*/ fun toShort(): kotlin.Short
    public final operator fun unaryMinus(): kotlin.Long
    public final operator fun unaryPlus(): kotlin.Long
    public final infix fun ushr(/*0*/ bitCount: kotlin.Int): kotlin.Long
    public final infix fun xor(/*0*/ other: kotlin.Long): kotlin.Long

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Long = 9223372036854775807.toLong()
            public final fun <get-MAX_VALUE>(): kotlin.Long
        public const final val MIN_VALUE: kotlin.Long = -9223372036854775808.toLong()
            public final fun <get-MIN_VALUE>(): kotlin.Long
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BITS: kotlin.Int = 64
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BYTES: kotlin.Int = 8
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class LongArray {
    /*primary*/ public constructor LongArray(/*0*/ size: kotlin.Int)
    public constructor LongArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Long)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Long
    public final operator fun iterator(): kotlin.collections.LongIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Long): kotlin.Unit
}

public open class NoSuchElementException : kotlin.RuntimeException {
    public constructor NoSuchElementException()
    /*primary*/ public constructor NoSuchElementException(/*0*/ message: kotlin.String?)
}

public open class NoWhenBranchMatchedException : kotlin.RuntimeException {
    public constructor NoWhenBranchMatchedException()
    public constructor NoWhenBranchMatchedException(/*0*/ message: kotlin.String?)
    /*primary*/ public constructor NoWhenBranchMatchedException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor NoWhenBranchMatchedException(/*0*/ cause: kotlin.Throwable?)
}

public final class NotImplementedError : kotlin.Error {
    /*primary*/ public constructor NotImplementedError(/*0*/ message: kotlin.String = ...)
}

public final class Nothing {
}

public open class NullPointerException : kotlin.RuntimeException {
    public constructor NullPointerException()
    /*primary*/ public constructor NullPointerException(/*0*/ message: kotlin.String?)
}

public abstract class Number {
    /*primary*/ public constructor Number()
    public abstract fun toByte(): kotlin.Byte
    public abstract fun toChar(): kotlin.Char
    public abstract fun toDouble(): kotlin.Double
    public abstract fun toFloat(): kotlin.Float
    public abstract fun toInt(): kotlin.Int
    public abstract fun toLong(): kotlin.Long
    public abstract fun toShort(): kotlin.Short
}

public open class NumberFormatException : kotlin.IllegalArgumentException {
    public constructor NumberFormatException()
    /*primary*/ public constructor NumberFormatException(/*0*/ message: kotlin.String?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) @kotlin.SinceKotlin(version = "1.3") public final annotation class OptIn : kotlin.Annotation {
    /*primary*/ public constructor OptIn(/*0*/ vararg markerClass: kotlin.reflect.KClass<out kotlin.Annotation> /*kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>*/)
    public final val markerClass: kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>
        public final fun <get-markerClass>(): kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.ExperimentalMultiplatform public final annotation class OptionalExpectation : kotlin.Annotation {
    /*primary*/ public constructor OptionalExpectation()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.4") @kotlin.experimental.ExperimentalTypeInference public final annotation class OverloadResolutionByLambdaReturnType : kotlin.Annotation {
    /*primary*/ public constructor OverloadResolutionByLambdaReturnType()
}

public final data class Pair</*0*/ out A, /*1*/ out B> : kotlin.io.Serializable {
    /*primary*/ public constructor Pair</*0*/ out A, /*1*/ out B>(/*0*/ first: A, /*1*/ second: B)
    public final val first: A
        public final fun <get-first>(): A
    public final val second: B
        public final fun <get-second>(): B
    public final operator /*synthesized*/ fun component1(): A
    public final operator /*synthesized*/ fun component2(): B
    public final /*synthesized*/ fun copy(/*0*/ first: A = ..., /*1*/ second: B = ...): kotlin.Pair<A, B>
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun toString(): kotlin.String
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE}) @kotlin.annotation.MustBeDocumented @kotlin.SinceKotlin(version = "1.1") public final annotation class ParameterName : kotlin.Annotation {
    /*primary*/ public constructor ParameterName(/*0*/ name: kotlin.String)
    public final val name: kotlin.String
        public final fun <get-name>(): kotlin.String
}
