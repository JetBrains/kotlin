@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public val kotlin.Char.code: kotlin.Int { get; }

@kotlin.SinceKotlin(version = "1.2")
@kotlin.internal.InlineOnly
public val kotlin.reflect.KProperty0<*>.isInitialized: kotlin.Boolean { get; }

@kotlin.SinceKotlin(version = "1.4")
public val kotlin.Throwable.suppressedExceptions: kotlin.collections.List<kotlin.Throwable> { get; }

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun Char(code: kotlin.Int): kotlin.Char

@kotlin.internal.InlineOnly
public inline fun TODO(): kotlin.Nothing

@kotlin.internal.InlineOnly
public inline fun TODO(reason: kotlin.String): kotlin.Nothing

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByteArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.UByte): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UIntArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.UInt): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULongArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.ULong): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShortArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.UShort): kotlin.UShortArray

/*∆*/ public inline fun <T> arrayOf(vararg elements: T): kotlin.Array<T>

/*∆*/ public inline fun <reified T> arrayOfNulls(size: kotlin.Int): kotlin.Array<T?>

/*∆*/ public inline fun booleanArrayOf(vararg elements: kotlin.Boolean): kotlin.BooleanArray

/*∆*/ public inline fun byteArrayOf(vararg elements: kotlin.Byte): kotlin.ByteArray

/*∆*/ public inline fun charArrayOf(vararg elements: kotlin.Char): kotlin.CharArray

@kotlin.internal.InlineOnly
public inline fun check(value: kotlin.Boolean): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun check(value: kotlin.Boolean, lazyMessage: () -> kotlin.Any): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Any> checkNotNull(value: T?): T

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Any> checkNotNull(value: T?, lazyMessage: () -> kotlin.Any): T

/*∆*/ public inline fun doubleArrayOf(vararg elements: kotlin.Double): kotlin.DoubleArray

public inline fun <T> emptyArray(): kotlin.Array<T>

@kotlin.SinceKotlin(version = "1.1")
public inline fun <reified T : kotlin.Enum<T>> enumValueOf(name: kotlin.String): T

@kotlin.SinceKotlin(version = "1.1")
public inline fun <reified T : kotlin.Enum<T>> enumValues(): kotlin.Array<T>

@kotlin.internal.InlineOnly
public inline fun error(message: kotlin.Any): kotlin.Nothing

/*∆*/ public inline fun floatArrayOf(vararg elements: kotlin.Float): kotlin.FloatArray

/*∆*/ public inline fun intArrayOf(vararg elements: kotlin.Int): kotlin.IntArray

public fun <T> lazy(initializer: () -> T): kotlin.Lazy<T>

public fun <T> lazy(lock: kotlin.Any?, initializer: () -> T): kotlin.Lazy<T>

public fun <T> lazy(mode: kotlin.LazyThreadSafetyMode, initializer: () -> T): kotlin.Lazy<T>

public fun <T> lazyOf(value: T): kotlin.Lazy<T>

/*∆*/ public inline fun longArrayOf(vararg elements: kotlin.Long): kotlin.LongArray

@kotlin.internal.InlineOnly
public inline fun repeat(times: kotlin.Int, action: (kotlin.Int) -> kotlin.Unit): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun require(value: kotlin.Boolean): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun require(value: kotlin.Boolean, lazyMessage: () -> kotlin.Any): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Any> requireNotNull(value: T?): T

@kotlin.internal.InlineOnly
public inline fun <T : kotlin.Any> requireNotNull(value: T?, lazyMessage: () -> kotlin.Any): T

@kotlin.internal.InlineOnly
public inline fun <R> run(block: () -> R): R

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R> runCatching(block: () -> R): kotlin.Result<R>

/*∆*/ public inline fun shortArrayOf(vararg elements: kotlin.Short): kotlin.ShortArray

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.2")
public inline fun <R> suspend(noinline block: suspend () -> R): suspend () -> R

@kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
@kotlin.Deprecated(message = "Synchronization on any object is not supported in Kotlin/JS", replaceWith = kotlin.ReplaceWith(expression = "run(block)", imports = {}))
@kotlin.internal.InlineOnly
public inline fun <R> synchronized(lock: kotlin.Any, block: () -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ubyteArrayOf(vararg elements: kotlin.UByte): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun uintArrayOf(vararg elements: kotlin.UInt): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ulongArrayOf(vararg elements: kotlin.ULong): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ushortArrayOf(vararg elements: kotlin.UShort): kotlin.UShortArray

@kotlin.internal.InlineOnly
public inline fun <T, R> with(receiver: T, block: T.() -> R): R

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Throwable.addSuppressed(exception: kotlin.Throwable): kotlin.Unit

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.1")
public inline fun <T> T.also(block: (T) -> kotlin.Unit): T

@kotlin.internal.InlineOnly
public inline fun <T> T.apply(block: T.() -> kotlin.Unit): T

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.6")
public inline infix fun <T> kotlin.Comparable<T>.compareTo(other: T): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.countLeadingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.countOneBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.countTrailingZeroBits(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.floorDiv(other: kotlin.Byte): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.floorDiv(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.floorDiv(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.floorDiv(other: kotlin.Short): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.floorDiv(other: kotlin.Byte): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.floorDiv(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.floorDiv(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.floorDiv(other: kotlin.Short): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.floorDiv(other: kotlin.Byte): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.floorDiv(other: kotlin.Int): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.floorDiv(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.floorDiv(other: kotlin.Short): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.floorDiv(other: kotlin.Byte): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.floorDiv(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.floorDiv(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.floorDiv(other: kotlin.Short): kotlin.Int

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T> kotlin.Result<T>.fold(onSuccess: (value: T) -> R, onFailure: (exception: kotlin.Throwable) -> R): R

@kotlin.SinceKotlin(version = "1.2")
@kotlin.internal.InlineOnly
public inline fun kotlin.Double.Companion.fromBits(bits: kotlin.Long): kotlin.Double

@kotlin.SinceKotlin(version = "1.2")
@kotlin.internal.InlineOnly
public inline fun kotlin.Float.Companion.fromBits(bits: kotlin.Int): kotlin.Float

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T : R> kotlin.Result<T>.getOrDefault(defaultValue: R): R

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T : R> kotlin.Result<T>.getOrElse(onFailure: (exception: kotlin.Throwable) -> R): R

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <T> kotlin.Result<T>.getOrThrow(): T

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Lazy<T>.getValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): T

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline operator fun <V> kotlin.reflect.KProperty0<V>.getValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): V

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline operator fun <T, V> kotlin.reflect.KProperty1<T, V>.getValue(thisRef: T, property: kotlin.reflect.KProperty<*>): V

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.Any?.hashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalStdlibApi
public operator fun <T, R> kotlin.DeepRecursiveFunction<T, R>.invoke(value: T): R

public fun kotlin.Double.isFinite(): kotlin.Boolean

public fun kotlin.Float.isFinite(): kotlin.Boolean

public fun kotlin.Double.isInfinite(): kotlin.Boolean

public fun kotlin.Float.isInfinite(): kotlin.Boolean

public fun kotlin.Double.isNaN(): kotlin.Boolean

public fun kotlin.Float.isNaN(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <T, R> T.let(block: (T) -> R): R

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T> kotlin.Result<T>.map(transform: (value: T) -> R): kotlin.Result<R>

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T> kotlin.Result<T>.mapCatching(transform: (value: T) -> R): kotlin.Result<R>

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.mod(other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.mod(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.mod(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.mod(other: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Double.mod(other: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Double.mod(other: kotlin.Float): kotlin.Double

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Float.mod(other: kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Float.mod(other: kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.mod(other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.mod(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.mod(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.mod(other: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.mod(other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.mod(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.mod(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.mod(other: kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.mod(other: kotlin.Byte): kotlin.Byte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.mod(other: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.mod(other: kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.mod(other: kotlin.Short): kotlin.Short

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <T> kotlin.Result<T>.onFailure(action: (exception: kotlin.Throwable) -> kotlin.Unit): kotlin.Result<T>

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <T> kotlin.Result<T>.onSuccess(action: (value: T) -> kotlin.Unit): kotlin.Result<T>

public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Throwable.printStackTrace(): kotlin.Unit

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T : R> kotlin.Result<T>.recover(transform: (exception: kotlin.Throwable) -> R): kotlin.Result<R>

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <R, T : R> kotlin.Result<T>.recoverCatching(transform: (exception: kotlin.Throwable) -> R): kotlin.Result<R>

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Byte.rotateLeft(bitCount: kotlin.Int): kotlin.Byte

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.rotateLeft(bitCount: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.rotateLeft(bitCount: kotlin.Int): kotlin.Long

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Short.rotateLeft(bitCount: kotlin.Int): kotlin.Short

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.rotateLeft(bitCount: kotlin.Int): kotlin.UByte

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.rotateLeft(bitCount: kotlin.Int): kotlin.UInt

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.rotateLeft(bitCount: kotlin.Int): kotlin.ULong

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.rotateLeft(bitCount: kotlin.Int): kotlin.UShort

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Byte.rotateRight(bitCount: kotlin.Int): kotlin.Byte

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.rotateRight(bitCount: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.rotateRight(bitCount: kotlin.Int): kotlin.Long

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Short.rotateRight(bitCount: kotlin.Int): kotlin.Short

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.rotateRight(bitCount: kotlin.Int): kotlin.UByte

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.rotateRight(bitCount: kotlin.Int): kotlin.UInt

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.rotateRight(bitCount: kotlin.Int): kotlin.ULong

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class, kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.rotateRight(bitCount: kotlin.Int): kotlin.UShort

@kotlin.internal.InlineOnly
public inline fun <T, R> T.run(block: T.() -> R): R

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.3")
public inline fun <T, R> T.runCatching(block: T.() -> R): kotlin.Result<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline operator fun <V> kotlin.reflect.KMutableProperty0<V>.setValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>, value: V): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline operator fun <T, V> kotlin.reflect.KMutableProperty1<T, V>.setValue(thisRef: T, property: kotlin.reflect.KProperty<*>, value: V): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Throwable.stackTraceToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.takeHighestOneBit(): kotlin.Byte

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.takeHighestOneBit(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.takeHighestOneBit(): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.takeHighestOneBit(): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.takeHighestOneBit(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.takeHighestOneBit(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.takeHighestOneBit(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.takeHighestOneBit(): kotlin.UShort

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.1")
public inline fun <T> T.takeIf(predicate: (T) -> kotlin.Boolean): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.takeLowestOneBit(): kotlin.Byte

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.takeLowestOneBit(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Long.takeLowestOneBit(): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.takeLowestOneBit(): kotlin.Short

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.takeLowestOneBit(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.takeLowestOneBit(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.takeLowestOneBit(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class, kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.takeLowestOneBit(): kotlin.UShort

@kotlin.internal.InlineOnly
@kotlin.SinceKotlin(version = "1.1")
public inline fun <T> T.takeUnless(predicate: (T) -> kotlin.Boolean): T?

public infix fun <A, B> A.to(that: B): kotlin.Pair<A, B>

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Double.toBits(): kotlin.Long

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Float.toBits(): kotlin.Int

public fun <T> kotlin.Pair<T, T>.toList(): kotlin.collections.List<T>

public fun <T> kotlin.Triple<T, T, T>.toList(): kotlin.collections.List<T>

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Double.toRawBits(): kotlin.Long

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Float.toRawBits(): kotlin.Int

public fun kotlin.Any?.toString(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.toUByte(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.toUByte(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.toUByte(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.toUByte(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Double.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Float.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Double.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Float.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.toUShort(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Int.toUShort(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Long.toUShort(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.toUShort(): kotlin.UShort

public interface Annotation {
}

public open class Any {
    public constructor Any()

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open fun hashCode(): kotlin.Int

    public open fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3")
public open class ArithmeticException : kotlin.RuntimeException {
    public constructor ArithmeticException()

    public constructor ArithmeticException(message: kotlin.String?)
}

public final class Array<T> {
    public constructor Array<T>(size: kotlin.Int, init: (kotlin.Int) -> T)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): T

    public final operator fun iterator(): kotlin.collections.Iterator<T>

    public final operator fun set(index: kotlin.Int, value: T): kotlin.Unit
}

public open class AssertionError : kotlin.Error {
    public constructor AssertionError()

    public constructor AssertionError(message: kotlin.Any?)

    public constructor AssertionError(message: kotlin.String?)

    @kotlin.SinceKotlin(version = "1.4")
    public constructor AssertionError(message: kotlin.String?, cause: kotlin.Throwable?)
}

public final class Boolean : kotlin.Comparable<kotlin.Boolean> {
    public final infix fun and(other: kotlin.Boolean): kotlin.Boolean

    public open override operator fun compareTo(other: kotlin.Boolean): kotlin.Int

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun not(): kotlin.Boolean

    public final infix fun or(other: kotlin.Boolean): kotlin.Boolean

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final infix fun xor(other: kotlin.Boolean): kotlin.Boolean

    @kotlin.SinceKotlin(version = "1.3")
    public companion object of Boolean {
    }
}

public final class BooleanArray {
    public constructor BooleanArray(size: kotlin.Int)

    public constructor BooleanArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Boolean)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Boolean

    public final operator fun iterator(): kotlin.collections.BooleanIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Boolean): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.3")
@kotlin.experimental.ExperimentalTypeInference
public final annotation class BuilderInference : kotlin.Annotation {
    public constructor BuilderInference()
}

public final class Byte : kotlin.Number, kotlin.Comparable<kotlin.Byte> {
    public open override operator fun compareTo(other: kotlin.Byte): kotlin.Int

    public final operator fun compareTo(other: kotlin.Double): kotlin.Int

    public final operator fun compareTo(other: kotlin.Float): kotlin.Int

    public final operator fun compareTo(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Long): kotlin.Int

    public final operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Byte

    public final operator fun div(other: kotlin.Byte): kotlin.Int

    public final operator fun div(other: kotlin.Double): kotlin.Double

    public final operator fun div(other: kotlin.Float): kotlin.Float

    public final operator fun div(other: kotlin.Int): kotlin.Int

    public final operator fun div(other: kotlin.Long): kotlin.Long

    public final operator fun div(other: kotlin.Short): kotlin.Int

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Byte

    public final operator fun minus(other: kotlin.Byte): kotlin.Int

    public final operator fun minus(other: kotlin.Double): kotlin.Double

    public final operator fun minus(other: kotlin.Float): kotlin.Float

    public final operator fun minus(other: kotlin.Int): kotlin.Int

    public final operator fun minus(other: kotlin.Long): kotlin.Long

    public final operator fun minus(other: kotlin.Short): kotlin.Int

    public final operator fun plus(other: kotlin.Byte): kotlin.Int

    public final operator fun plus(other: kotlin.Double): kotlin.Double

    public final operator fun plus(other: kotlin.Float): kotlin.Float

    public final operator fun plus(other: kotlin.Int): kotlin.Int

    public final operator fun plus(other: kotlin.Long): kotlin.Long

    public final operator fun plus(other: kotlin.Short): kotlin.Int

    public final operator fun rangeTo(other: kotlin.Byte): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Int): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Long): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Short): kotlin.ranges.IntRange

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Byte): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Float): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Int): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Short): kotlin.Int

    public final operator fun times(other: kotlin.Byte): kotlin.Int

    public final operator fun times(other: kotlin.Double): kotlin.Double

    public final operator fun times(other: kotlin.Float): kotlin.Float

    public final operator fun times(other: kotlin.Int): kotlin.Int

    public final operator fun times(other: kotlin.Long): kotlin.Long

    public final operator fun times(other: kotlin.Short): kotlin.Int

    public open override fun toByte(): kotlin.Byte

    @kotlin.Deprecated(message = "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.", replaceWith = kotlin.ReplaceWith(expression = "this.toInt().toChar()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Int

    public final operator fun unaryPlus(): kotlin.Int

    public companion object of Byte {
        public const final val MAX_VALUE: kotlin.Byte = 127.toByte() { get; }

        public const final val MIN_VALUE: kotlin.Byte = -128.toByte() { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BITS: kotlin.Int = 8 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BYTES: kotlin.Int = 1 { get; }
    }
}

public final class ByteArray {
    public constructor ByteArray(size: kotlin.Int)

    public constructor ByteArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Byte)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Byte

    public final operator fun iterator(): kotlin.collections.ByteIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Byte): kotlin.Unit
}

public final class Char : kotlin.Comparable<kotlin.Char> {
/*∆*/     @kotlin.SinceKotlin(version = "1.5")
/*∆*/     @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
/*∆*/     public constructor Char(code: kotlin.UShort)
/*∆*/ 
    public open override operator fun compareTo(other: kotlin.Char): kotlin.Int

    public final operator fun dec(): kotlin.Char

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Char

    public final operator fun minus(other: kotlin.Char): kotlin.Int

    public final operator fun minus(other: kotlin.Int): kotlin.Char

    public final operator fun plus(other: kotlin.Int): kotlin.Char

    public final operator fun rangeTo(other: kotlin.Char): kotlin.ranges.CharRange

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code.toByte()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toByte(): kotlin.Byte

    public final fun toChar(): kotlin.Char

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code.toDouble()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toDouble(): kotlin.Double

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code.toFloat()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toFloat(): kotlin.Float

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toInt(): kotlin.Int

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code.toLong()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toLong(): kotlin.Long

    @kotlin.Deprecated(message = "Conversion of Char to Number is deprecated. Use Char.code property instead.", replaceWith = kotlin.ReplaceWith(expression = "this.code.toShort()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public final fun toShort(): kotlin.Short

/*∆*/     @kotlin.js.JsName(name = "toString")
/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public companion object of Char {
        public const final val MAX_HIGH_SURROGATE: kotlin.Char = \uDBFF ('?') { get; }

        public const final val MAX_LOW_SURROGATE: kotlin.Char = \uDFFF ('?') { get; }

        public const final val MAX_SURROGATE: kotlin.Char = \uDFFF ('?') { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val MAX_VALUE: kotlin.Char = \uFFFF ('?') { get; }

        public const final val MIN_HIGH_SURROGATE: kotlin.Char = \uD800 ('?') { get; }

        public const final val MIN_LOW_SURROGATE: kotlin.Char = \uDC00 ('?') { get; }

        public const final val MIN_SURROGATE: kotlin.Char = \uD800 ('?') { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val MIN_VALUE: kotlin.Char = \u0000 ('?') { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BITS: kotlin.Int = 16 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BYTES: kotlin.Int = 2 { get; }
    }
}

public final class CharArray {
    public constructor CharArray(size: kotlin.Int)

    public constructor CharArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Char)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Char

    public final operator fun iterator(): kotlin.collections.CharIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Char): kotlin.Unit
}

public interface CharSequence {
    public abstract val length: kotlin.Int { get; }

    public abstract operator fun get(index: kotlin.Int): kotlin.Char

    public abstract fun subSequence(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.CharSequence
}

public open class ClassCastException : kotlin.RuntimeException {
    public constructor ClassCastException()

    public constructor ClassCastException(message: kotlin.String?)
}

public interface Comparable<in T> {
    public abstract operator fun compareTo(other: T): kotlin.Int
}

public fun interface Comparator<T> {
    @kotlin.js.JsName(name = "compare")
    public abstract fun compare(a: T, b: T): kotlin.Int
}

public open class ConcurrentModificationException : kotlin.RuntimeException {
    public constructor ConcurrentModificationException()

    public constructor ConcurrentModificationException(message: kotlin.String?)

    public constructor ConcurrentModificationException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor ConcurrentModificationException(cause: kotlin.Throwable?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE})
@kotlin.annotation.MustBeDocumented
public final annotation class ContextFunctionTypeParams : kotlin.Annotation {
    public constructor ContextFunctionTypeParams(count: kotlin.Int)

    public final val count: kotlin.Int { get; }
}

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalStdlibApi
public final class DeepRecursiveFunction<T, R> {
    public constructor DeepRecursiveFunction<T, R>(block: suspend kotlin.DeepRecursiveScope<T, R>.(T) -> R)
}

@kotlin.coroutines.RestrictsSuspension
@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalStdlibApi
public sealed class DeepRecursiveScope<T, R> {
    protected constructor DeepRecursiveScope<T, R>()

    public abstract suspend fun callRecursive(value: T): R

    public abstract suspend fun <U, S> kotlin.DeepRecursiveFunction<U, S>.callRecursive(value: U): S

    @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "'invoke' should not be called from DeepRecursiveScope. Use 'callRecursive' to do recursion in the heap instead of the call stack.", replaceWith = kotlin.ReplaceWith(expression = "this.callRecursive(value)", imports = {}))
    public final operator fun kotlin.DeepRecursiveFunction<*, *>.invoke(value: kotlin.Any?): kotlin.Nothing
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.MustBeDocumented
public final annotation class Deprecated : kotlin.Annotation {
    public constructor Deprecated(message: kotlin.String, replaceWith: kotlin.ReplaceWith = ..., level: kotlin.DeprecationLevel = ...)

    public final val level: kotlin.DeprecationLevel { get; }

    public final val message: kotlin.String { get; }

    public final val replaceWith: kotlin.ReplaceWith { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.4")
public final annotation class DeprecatedSinceKotlin : kotlin.Annotation {
    public constructor DeprecatedSinceKotlin(warningSince: kotlin.String = ..., errorSince: kotlin.String = ..., hiddenSince: kotlin.String = ...)

    public final val errorSince: kotlin.String { get; }

    public final val hiddenSince: kotlin.String { get; }

    public final val warningSince: kotlin.String { get; }
}

public final enum class DeprecationLevel : kotlin.Enum<kotlin.DeprecationLevel> {
    enum entry WARNING

    enum entry ERROR

    enum entry HIDDEN
}

public final class Double : kotlin.Number, kotlin.Comparable<kotlin.Double> {
    public final operator fun compareTo(other: kotlin.Byte): kotlin.Int

    public open override operator fun compareTo(other: kotlin.Double): kotlin.Int

    public final operator fun compareTo(other: kotlin.Float): kotlin.Int

    public final operator fun compareTo(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Long): kotlin.Int

    public final operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Double

    public final operator fun div(other: kotlin.Byte): kotlin.Double

    public final operator fun div(other: kotlin.Double): kotlin.Double

    public final operator fun div(other: kotlin.Float): kotlin.Double

    public final operator fun div(other: kotlin.Int): kotlin.Double

    public final operator fun div(other: kotlin.Long): kotlin.Double

    public final operator fun div(other: kotlin.Short): kotlin.Double

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Double

    public final operator fun minus(other: kotlin.Byte): kotlin.Double

    public final operator fun minus(other: kotlin.Double): kotlin.Double

    public final operator fun minus(other: kotlin.Float): kotlin.Double

    public final operator fun minus(other: kotlin.Int): kotlin.Double

    public final operator fun minus(other: kotlin.Long): kotlin.Double

    public final operator fun minus(other: kotlin.Short): kotlin.Double

    public final operator fun plus(other: kotlin.Byte): kotlin.Double

    public final operator fun plus(other: kotlin.Double): kotlin.Double

    public final operator fun plus(other: kotlin.Float): kotlin.Double

    public final operator fun plus(other: kotlin.Int): kotlin.Double

    public final operator fun plus(other: kotlin.Long): kotlin.Double

    public final operator fun plus(other: kotlin.Short): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Byte): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Float): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Int): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Short): kotlin.Double

    public final operator fun times(other: kotlin.Byte): kotlin.Double

    public final operator fun times(other: kotlin.Double): kotlin.Double

    public final operator fun times(other: kotlin.Float): kotlin.Double

    public final operator fun times(other: kotlin.Int): kotlin.Double

    public final operator fun times(other: kotlin.Long): kotlin.Double

    public final operator fun times(other: kotlin.Short): kotlin.Double

    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toByte()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.3")
    public open override fun toByte(): kotlin.Byte

    @kotlin.Deprecated(message = "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.", replaceWith = kotlin.ReplaceWith(expression = "this.toInt().toChar()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toShort()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.3")
    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Double

    public final operator fun unaryPlus(): kotlin.Double

    public companion object of Double {
        public const final val MAX_VALUE: kotlin.Double = 1.7976931348623157E308.toDouble() { get; }

        public const final val MIN_VALUE: kotlin.Double = 4.9E-324.toDouble() { get; }

        public const final val NEGATIVE_INFINITY: kotlin.Double = -Infinity.toDouble() { get; }

        public const final val NaN: kotlin.Double = NaN.toDouble() { get; }

        public const final val POSITIVE_INFINITY: kotlin.Double = Infinity.toDouble() { get; }

        @kotlin.SinceKotlin(version = "1.4")
        public const final val SIZE_BITS: kotlin.Int = 64 { get; }

        @kotlin.SinceKotlin(version = "1.4")
        public const final val SIZE_BYTES: kotlin.Int = 8 { get; }
    }
}

public final class DoubleArray {
    public constructor DoubleArray(size: kotlin.Int)

    public constructor DoubleArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Double)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Double

    public final operator fun iterator(): kotlin.collections.DoubleIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Double): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.1")
public final annotation class DslMarker : kotlin.Annotation {
    public constructor DslMarker()
}

public abstract class Enum<E : kotlin.Enum<E>> : kotlin.Comparable<E> {
    public constructor Enum<E : kotlin.Enum<E>>(name: kotlin.String, ordinal: kotlin.Int)

    public final val name: kotlin.String { get; }

    public final val ordinal: kotlin.Int { get; }

    public final override operator fun compareTo(other: E): kotlin.Int

    public final override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String

    public companion object of Enum {
    }
}

public open class Error : kotlin.Throwable {
    public constructor Error()

    public constructor Error(message: kotlin.String?)

    public constructor Error(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor Error(cause: kotlin.Throwable?)
}

public open class Exception : kotlin.Throwable {
    public constructor Exception()

    public constructor Exception(message: kotlin.String?)

    public constructor Exception(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor Exception(cause: kotlin.Throwable?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.2")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.6", warningSince = "1.4")
@kotlin.Deprecated(message = "Please use RequiresOptIn instead.")
public final annotation class Experimental : kotlin.Annotation {
    public constructor Experimental(level: kotlin.Experimental.Level = ...)

    public final val level: kotlin.Experimental.Level { get; }

    public final enum class Level : kotlin.Enum<kotlin.Experimental.Level> {
        enum entry WARNING

        enum entry ERROR
    }
}

@kotlin.RequiresOptIn
@kotlin.annotation.MustBeDocumented
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
public final annotation class ExperimentalMultiplatform : kotlin.Annotation {
    public constructor ExperimentalMultiplatform()
}

@kotlin.RequiresOptIn(level = Level.ERROR)
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.3")
public final annotation class ExperimentalStdlibApi : kotlin.Annotation {
    public constructor ExperimentalStdlibApi()
}

@kotlin.RequiresOptIn(level = Level.WARNING)
@kotlin.annotation.MustBeDocumented
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
public final annotation class ExperimentalUnsignedTypes : kotlin.Annotation {
    public constructor ExperimentalUnsignedTypes()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE})
@kotlin.annotation.MustBeDocumented
public final annotation class ExtensionFunctionType : kotlin.Annotation {
    public constructor ExtensionFunctionType()
}

public final class Float : kotlin.Number, kotlin.Comparable<kotlin.Float> {
    public final operator fun compareTo(other: kotlin.Byte): kotlin.Int

    public final operator fun compareTo(other: kotlin.Double): kotlin.Int

    public open override operator fun compareTo(other: kotlin.Float): kotlin.Int

    public final operator fun compareTo(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Long): kotlin.Int

    public final operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Float

    public final operator fun div(other: kotlin.Byte): kotlin.Float

    public final operator fun div(other: kotlin.Double): kotlin.Double

    public final operator fun div(other: kotlin.Float): kotlin.Float

    public final operator fun div(other: kotlin.Int): kotlin.Float

    public final operator fun div(other: kotlin.Long): kotlin.Float

    public final operator fun div(other: kotlin.Short): kotlin.Float

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Float

    public final operator fun minus(other: kotlin.Byte): kotlin.Float

    public final operator fun minus(other: kotlin.Double): kotlin.Double

    public final operator fun minus(other: kotlin.Float): kotlin.Float

    public final operator fun minus(other: kotlin.Int): kotlin.Float

    public final operator fun minus(other: kotlin.Long): kotlin.Float

    public final operator fun minus(other: kotlin.Short): kotlin.Float

    public final operator fun plus(other: kotlin.Byte): kotlin.Float

    public final operator fun plus(other: kotlin.Double): kotlin.Double

    public final operator fun plus(other: kotlin.Float): kotlin.Float

    public final operator fun plus(other: kotlin.Int): kotlin.Float

    public final operator fun plus(other: kotlin.Long): kotlin.Float

    public final operator fun plus(other: kotlin.Short): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Byte): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Float): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Int): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Short): kotlin.Float

    public final operator fun times(other: kotlin.Byte): kotlin.Float

    public final operator fun times(other: kotlin.Double): kotlin.Double

    public final operator fun times(other: kotlin.Float): kotlin.Float

    public final operator fun times(other: kotlin.Int): kotlin.Float

    public final operator fun times(other: kotlin.Long): kotlin.Float

    public final operator fun times(other: kotlin.Short): kotlin.Float

    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toByte()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.3")
    public open override fun toByte(): kotlin.Byte

    @kotlin.Deprecated(message = "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.", replaceWith = kotlin.ReplaceWith(expression = "this.toInt().toChar()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    @kotlin.Deprecated(message = "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.", replaceWith = kotlin.ReplaceWith(expression = "toInt().toShort()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.3")
    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Float

    public final operator fun unaryPlus(): kotlin.Float

    public companion object of Float {
        public const final val MAX_VALUE: kotlin.Float = 3.4028235E38.toFloat() { get; }

        public const final val MIN_VALUE: kotlin.Float = 1.4E-45.toFloat() { get; }

        public const final val NEGATIVE_INFINITY: kotlin.Float = -Infinity.toFloat() { get; }

        public const final val NaN: kotlin.Float = NaN.toFloat() { get; }

        public const final val POSITIVE_INFINITY: kotlin.Float = Infinity.toFloat() { get; }

        @kotlin.SinceKotlin(version = "1.4")
        public const final val SIZE_BITS: kotlin.Int = 32 { get; }

        @kotlin.SinceKotlin(version = "1.4")
        public const final val SIZE_BYTES: kotlin.Int = 4 { get; }
    }
}

public final class FloatArray {
    public constructor FloatArray(size: kotlin.Int)

    public constructor FloatArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Float)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Float

    public final operator fun iterator(): kotlin.collections.FloatIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Float): kotlin.Unit
}

public interface Function<out R> {
}

public open class IllegalArgumentException : kotlin.RuntimeException {
    public constructor IllegalArgumentException()

    public constructor IllegalArgumentException(message: kotlin.String?)

    public constructor IllegalArgumentException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor IllegalArgumentException(cause: kotlin.Throwable?)
}

public open class IllegalStateException : kotlin.RuntimeException {
    public constructor IllegalStateException()

    public constructor IllegalStateException(message: kotlin.String?)

    public constructor IllegalStateException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor IllegalStateException(cause: kotlin.Throwable?)
}

public open class IndexOutOfBoundsException : kotlin.RuntimeException {
    public constructor IndexOutOfBoundsException()

    public constructor IndexOutOfBoundsException(message: kotlin.String?)
}

public final class Int : kotlin.Number, kotlin.Comparable<kotlin.Int> {
    public final infix fun and(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Byte): kotlin.Int

    public final operator fun compareTo(other: kotlin.Double): kotlin.Int

    public final operator fun compareTo(other: kotlin.Float): kotlin.Int

    public open override operator fun compareTo(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Long): kotlin.Int

    public final operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Int

    public final operator fun div(other: kotlin.Byte): kotlin.Int

    public final operator fun div(other: kotlin.Double): kotlin.Double

    public final operator fun div(other: kotlin.Float): kotlin.Float

    public final operator fun div(other: kotlin.Int): kotlin.Int

    public final operator fun div(other: kotlin.Long): kotlin.Long

    public final operator fun div(other: kotlin.Short): kotlin.Int

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Int

    public final fun inv(): kotlin.Int

    public final operator fun minus(other: kotlin.Byte): kotlin.Int

    public final operator fun minus(other: kotlin.Double): kotlin.Double

    public final operator fun minus(other: kotlin.Float): kotlin.Float

    public final operator fun minus(other: kotlin.Int): kotlin.Int

    public final operator fun minus(other: kotlin.Long): kotlin.Long

    public final operator fun minus(other: kotlin.Short): kotlin.Int

    public final infix fun or(other: kotlin.Int): kotlin.Int

    public final operator fun plus(other: kotlin.Byte): kotlin.Int

    public final operator fun plus(other: kotlin.Double): kotlin.Double

    public final operator fun plus(other: kotlin.Float): kotlin.Float

    public final operator fun plus(other: kotlin.Int): kotlin.Int

    public final operator fun plus(other: kotlin.Long): kotlin.Long

    public final operator fun plus(other: kotlin.Short): kotlin.Int

    public final operator fun rangeTo(other: kotlin.Byte): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Int): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Long): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Short): kotlin.ranges.IntRange

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Byte): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Float): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Int): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Short): kotlin.Int

    public final infix fun shl(bitCount: kotlin.Int): kotlin.Int

    public final infix fun shr(bitCount: kotlin.Int): kotlin.Int

    public final operator fun times(other: kotlin.Byte): kotlin.Int

    public final operator fun times(other: kotlin.Double): kotlin.Double

    public final operator fun times(other: kotlin.Float): kotlin.Float

    public final operator fun times(other: kotlin.Int): kotlin.Int

    public final operator fun times(other: kotlin.Long): kotlin.Long

    public final operator fun times(other: kotlin.Short): kotlin.Int

    public open override fun toByte(): kotlin.Byte

    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Int

    public final operator fun unaryPlus(): kotlin.Int

    public final infix fun ushr(bitCount: kotlin.Int): kotlin.Int

    public final infix fun xor(other: kotlin.Int): kotlin.Int

    public companion object of Int {
        public const final val MAX_VALUE: kotlin.Int = 2147483647 { get; }

        public const final val MIN_VALUE: kotlin.Int = -2147483648 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BITS: kotlin.Int = 32 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BYTES: kotlin.Int = 4 { get; }
    }
}

public final class IntArray {
    public constructor IntArray(size: kotlin.Int)

    public constructor IntArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Int)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Int

    public final operator fun iterator(): kotlin.collections.IntIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Int): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.1")
public final class KotlinVersion : kotlin.Comparable<kotlin.KotlinVersion> {
    public constructor KotlinVersion(major: kotlin.Int, minor: kotlin.Int)

    public constructor KotlinVersion(major: kotlin.Int, minor: kotlin.Int, patch: kotlin.Int)

    public final val major: kotlin.Int { get; }

    public final val minor: kotlin.Int { get; }

    public final val patch: kotlin.Int { get; }

    public open override operator fun compareTo(other: kotlin.KotlinVersion): kotlin.Int

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public final fun isAtLeast(major: kotlin.Int, minor: kotlin.Int): kotlin.Boolean

    public final fun isAtLeast(major: kotlin.Int, minor: kotlin.Int, patch: kotlin.Int): kotlin.Boolean

    public open override fun toString(): kotlin.String

    public companion object of KotlinVersion {
        public final val CURRENT: kotlin.KotlinVersion { get; }

        public const final val MAX_COMPONENT_VALUE: kotlin.Int = 255 { get; }
    }
}

public interface Lazy<out T> {
    public abstract val value: T { get; }

    public abstract fun isInitialized(): kotlin.Boolean
}

public final enum class LazyThreadSafetyMode : kotlin.Enum<kotlin.LazyThreadSafetyMode> {
    enum entry SYNCHRONIZED

    enum entry PUBLICATION

    enum entry NONE
}

public final class Long : kotlin.Number, kotlin.Comparable<kotlin.Long> {
    public final infix fun and(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun compareTo(other: kotlin.Byte): kotlin.Int

/*∆*/     public final inline operator fun compareTo(other: kotlin.Double): kotlin.Int

/*∆*/     public final inline operator fun compareTo(other: kotlin.Float): kotlin.Int

/*∆*/     public final inline operator fun compareTo(other: kotlin.Int): kotlin.Int

    public open override operator fun compareTo(other: kotlin.Long): kotlin.Int

/*∆*/     public final inline operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Long

/*∆*/     public final inline operator fun div(other: kotlin.Byte): kotlin.Long

/*∆*/     public final inline operator fun div(other: kotlin.Double): kotlin.Double

/*∆*/     public final inline operator fun div(other: kotlin.Float): kotlin.Float

/*∆*/     public final inline operator fun div(other: kotlin.Int): kotlin.Long

    public final operator fun div(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun div(other: kotlin.Short): kotlin.Long

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Long

    public final fun inv(): kotlin.Long

/*∆*/     public final inline operator fun minus(other: kotlin.Byte): kotlin.Long

/*∆*/     public final inline operator fun minus(other: kotlin.Double): kotlin.Double

/*∆*/     public final inline operator fun minus(other: kotlin.Float): kotlin.Float

/*∆*/     public final inline operator fun minus(other: kotlin.Int): kotlin.Long

    public final operator fun minus(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun minus(other: kotlin.Short): kotlin.Long

    public final infix fun or(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun plus(other: kotlin.Byte): kotlin.Long

/*∆*/     public final inline operator fun plus(other: kotlin.Double): kotlin.Double

/*∆*/     public final inline operator fun plus(other: kotlin.Float): kotlin.Float

/*∆*/     public final inline operator fun plus(other: kotlin.Int): kotlin.Long

    public final operator fun plus(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun plus(other: kotlin.Short): kotlin.Long

    public final operator fun rangeTo(other: kotlin.Byte): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Int): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Long): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Short): kotlin.ranges.LongRange

    @kotlin.SinceKotlin(version = "1.1")
/*∆*/     public final inline operator fun rem(other: kotlin.Byte): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
/*∆*/     public final inline operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
/*∆*/     public final inline operator fun rem(other: kotlin.Float): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
/*∆*/     public final inline operator fun rem(other: kotlin.Int): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
/*∆*/     public final inline operator fun rem(other: kotlin.Short): kotlin.Long

    public final infix fun shl(bitCount: kotlin.Int): kotlin.Long

    public final infix fun shr(bitCount: kotlin.Int): kotlin.Long

/*∆*/     public final inline operator fun times(other: kotlin.Byte): kotlin.Long

/*∆*/     public final inline operator fun times(other: kotlin.Double): kotlin.Double

/*∆*/     public final inline operator fun times(other: kotlin.Float): kotlin.Float

/*∆*/     public final inline operator fun times(other: kotlin.Int): kotlin.Long

    public final operator fun times(other: kotlin.Long): kotlin.Long

/*∆*/     public final inline operator fun times(other: kotlin.Short): kotlin.Long

    public open override fun toByte(): kotlin.Byte

    @kotlin.Deprecated(message = "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.", replaceWith = kotlin.ReplaceWith(expression = "this.toInt().toChar()", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Long

/*∆*/     public final inline operator fun unaryPlus(): kotlin.Long

    public final infix fun ushr(bitCount: kotlin.Int): kotlin.Long

    public final infix fun xor(other: kotlin.Long): kotlin.Long

    public companion object of Long {
        public const final val MAX_VALUE: kotlin.Long = 9223372036854775807.toLong() { get; }

        public const final val MIN_VALUE: kotlin.Long = -9223372036854775808.toLong() { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BITS: kotlin.Int = 64 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BYTES: kotlin.Int = 8 { get; }
    }
}

public final class LongArray {
    public constructor LongArray(size: kotlin.Int)

    public constructor LongArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Long)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Long

    public final operator fun iterator(): kotlin.collections.LongIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Long): kotlin.Unit
}

public open class NoSuchElementException : kotlin.RuntimeException {
    public constructor NoSuchElementException()

    public constructor NoSuchElementException(message: kotlin.String?)
}

public open class NoWhenBranchMatchedException : kotlin.RuntimeException {
    public constructor NoWhenBranchMatchedException()

    public constructor NoWhenBranchMatchedException(message: kotlin.String?)

    public constructor NoWhenBranchMatchedException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor NoWhenBranchMatchedException(cause: kotlin.Throwable?)
}

public final class NotImplementedError : kotlin.Error {
    public constructor NotImplementedError(message: kotlin.String = ...)
}

public final class Nothing {
}

public open class NullPointerException : kotlin.RuntimeException {
    public constructor NullPointerException()

    public constructor NullPointerException(message: kotlin.String?)
}

public abstract class Number {
    public constructor Number()

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

    public constructor NumberFormatException(message: kotlin.String?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
@kotlin.SinceKotlin(version = "1.3")
public final annotation class OptIn : kotlin.Annotation {
    public constructor OptIn(vararg markerClass: kotlin.reflect.KClass<out kotlin.Annotation>)

    public final val markerClass: kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>> { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.ExperimentalMultiplatform
public final annotation class OptionalExpectation : kotlin.Annotation {
    public constructor OptionalExpectation()
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.FUNCTION})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.4")
@kotlin.experimental.ExperimentalTypeInference
public final annotation class OverloadResolutionByLambdaReturnType : kotlin.Annotation {
    public constructor OverloadResolutionByLambdaReturnType()
}

public final data class Pair<out A, out B> : kotlin.io.Serializable {
    public constructor Pair<out A, out B>(first: A, second: B)

    public final val first: A { get; }

    public final val second: B { get; }

    public final operator fun component1(): A

    public final operator fun component2(): B

    public final fun copy(first: A = ..., second: B = ...): kotlin.Pair<A, B>

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE})
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.1")
public final annotation class ParameterName : kotlin.Annotation {
    public constructor ParameterName(name: kotlin.String)

    public final val name: kotlin.String { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.MustBeDocumented
@kotlin.SinceKotlin(version = "1.1")
public final annotation class PublishedApi : kotlin.Annotation {
    public constructor PublishedApi()
}

@kotlin.annotation.Target(allowedTargets = {})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.MustBeDocumented
public final annotation class ReplaceWith : kotlin.Annotation {
    public constructor ReplaceWith(expression: kotlin.String, vararg imports: kotlin.String)

    public final val expression: kotlin.String { get; }

    public final val imports: kotlin.Array<out kotlin.String> { get; }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.SinceKotlin(version = "1.3")
public final annotation class RequiresOptIn : kotlin.Annotation {
    public constructor RequiresOptIn(message: kotlin.String = ..., level: kotlin.RequiresOptIn.Level = ...)

    public final val level: kotlin.RequiresOptIn.Level { get; }

    public final val message: kotlin.String { get; }

    public final enum class Level : kotlin.Enum<kotlin.RequiresOptIn.Level> {
        enum entry WARNING

        enum entry ERROR
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.jvm.JvmInline
public final inline class Result<out T> : kotlin.io.Serializable {
    public final val isFailure: kotlin.Boolean { get; }

    public final val isSuccess: kotlin.Boolean { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final fun exceptionOrNull(): kotlin.Throwable?

    @kotlin.internal.InlineOnly
    public final inline fun getOrNull(): T?

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String

    public companion object of Result {
        @kotlin.internal.InlineOnly
        @kotlin.jvm.JvmName(name = "failure")
        public final inline fun <T> failure(exception: kotlin.Throwable): kotlin.Result<T>

        @kotlin.internal.InlineOnly
        @kotlin.jvm.JvmName(name = "success")
        public final inline fun <T> success(value: T): kotlin.Result<T>
    }
}

public open class RuntimeException : kotlin.Exception {
    public constructor RuntimeException()

    public constructor RuntimeException(message: kotlin.String?)

    public constructor RuntimeException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor RuntimeException(cause: kotlin.Throwable?)
}

public final class Short : kotlin.Number, kotlin.Comparable<kotlin.Short> {
    public final operator fun compareTo(other: kotlin.Byte): kotlin.Int

    public final operator fun compareTo(other: kotlin.Double): kotlin.Int

    public final operator fun compareTo(other: kotlin.Float): kotlin.Int

    public final operator fun compareTo(other: kotlin.Int): kotlin.Int

    public final operator fun compareTo(other: kotlin.Long): kotlin.Int

    public open override operator fun compareTo(other: kotlin.Short): kotlin.Int

    public final operator fun dec(): kotlin.Short

    public final operator fun div(other: kotlin.Byte): kotlin.Int

    public final operator fun div(other: kotlin.Double): kotlin.Double

    public final operator fun div(other: kotlin.Float): kotlin.Float

    public final operator fun div(other: kotlin.Int): kotlin.Int

    public final operator fun div(other: kotlin.Long): kotlin.Long

    public final operator fun div(other: kotlin.Short): kotlin.Int

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun inc(): kotlin.Short

    public final operator fun minus(other: kotlin.Byte): kotlin.Int

    public final operator fun minus(other: kotlin.Double): kotlin.Double

    public final operator fun minus(other: kotlin.Float): kotlin.Float

    public final operator fun minus(other: kotlin.Int): kotlin.Int

    public final operator fun minus(other: kotlin.Long): kotlin.Long

    public final operator fun minus(other: kotlin.Short): kotlin.Int

    public final operator fun plus(other: kotlin.Byte): kotlin.Int

    public final operator fun plus(other: kotlin.Double): kotlin.Double

    public final operator fun plus(other: kotlin.Float): kotlin.Float

    public final operator fun plus(other: kotlin.Int): kotlin.Int

    public final operator fun plus(other: kotlin.Long): kotlin.Long

    public final operator fun plus(other: kotlin.Short): kotlin.Int

    public final operator fun rangeTo(other: kotlin.Byte): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Int): kotlin.ranges.IntRange

    public final operator fun rangeTo(other: kotlin.Long): kotlin.ranges.LongRange

    public final operator fun rangeTo(other: kotlin.Short): kotlin.ranges.IntRange

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Byte): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Double): kotlin.Double

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Float): kotlin.Float

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Int): kotlin.Int

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Long): kotlin.Long

    @kotlin.SinceKotlin(version = "1.1")
    public final operator fun rem(other: kotlin.Short): kotlin.Int

    public final operator fun times(other: kotlin.Byte): kotlin.Int

    public final operator fun times(other: kotlin.Double): kotlin.Double

    public final operator fun times(other: kotlin.Float): kotlin.Float

    public final operator fun times(other: kotlin.Int): kotlin.Int

    public final operator fun times(other: kotlin.Long): kotlin.Long

    public final operator fun times(other: kotlin.Short): kotlin.Int

    public open override fun toByte(): kotlin.Byte

    @kotlin.Deprecated(message = "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.", replaceWith = kotlin.ReplaceWith(expression = "this.toInt().toChar()", imports = {}))
    public open override fun toChar(): kotlin.Char

    public open override fun toDouble(): kotlin.Double

    public open override fun toFloat(): kotlin.Float

    public open override fun toInt(): kotlin.Int

    public open override fun toLong(): kotlin.Long

    public open override fun toShort(): kotlin.Short

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public final operator fun unaryMinus(): kotlin.Int

    public final operator fun unaryPlus(): kotlin.Int

    public companion object of Short {
        public const final val MAX_VALUE: kotlin.Short = 32767.toShort() { get; }

        public const final val MIN_VALUE: kotlin.Short = -32768.toShort() { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BITS: kotlin.Int = 16 { get; }

        @kotlin.SinceKotlin(version = "1.3")
        public const final val SIZE_BYTES: kotlin.Int = 2 { get; }
    }
}

public final class ShortArray {
    public constructor ShortArray(size: kotlin.Int)

    public constructor ShortArray(size: kotlin.Int, init: (kotlin.Int) -> kotlin.Short)

    public final val size: kotlin.Int { get; }

    public final operator fun get(index: kotlin.Int): kotlin.Short

    public final operator fun iterator(): kotlin.collections.ShortIterator

    public final operator fun set(index: kotlin.Int, value: kotlin.Short): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
@kotlin.annotation.MustBeDocumented
public final annotation class SinceKotlin : kotlin.Annotation {
    public constructor SinceKotlin(version: kotlin.String)

    public final val version: kotlin.String { get; }
}

public final class String : kotlin.Comparable<kotlin.String>, kotlin.CharSequence {
    public constructor String()

    public open override val length: kotlin.Int { get; }

    public open override operator fun compareTo(other: kotlin.String): kotlin.Int

/*∆*/     public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean
/*∆*/ 
    public open override operator fun get(index: kotlin.Int): kotlin.Char

/*∆*/     public open override fun hashCode(): kotlin.Int
/*∆*/ 
    public final operator fun plus(other: kotlin.Any?): kotlin.String

    public open override fun subSequence(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.CharSequence

/*∆*/     public open override fun toString(): kotlin.String
/*∆*/ 
    public companion object of String {
    }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
public final annotation class Suppress : kotlin.Annotation {
    public constructor Suppress(vararg names: kotlin.String)

    public final val names: kotlin.Array<out kotlin.String> { get; }
}

/*∆*/ @kotlin.js.JsName(name = "Error")
/*∆*/ public open external class Throwable {
    public constructor Throwable()

    public constructor Throwable(message: kotlin.String?)

    public constructor Throwable(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor Throwable(cause: kotlin.Throwable?)

    public open val cause: kotlin.Throwable? { get; }

    public open val message: kotlin.String? { get; }
/*∆*/ 
/*∆*/     public open override fun toString(): kotlin.String
}

public final data class Triple<out A, out B, out C> : kotlin.io.Serializable {
    public constructor Triple<out A, out B, out C>(first: A, second: B, third: C)

    public final val first: A { get; }

    public final val second: B { get; }

    public final val third: C { get; }

    public final operator fun component1(): A

    public final operator fun component2(): B

    public final operator fun component3(): C

    public final fun copy(first: A = ..., second: B = ..., third: C = ...): kotlin.Triple<A, B, C>

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.jvm.JvmInline
public final inline class UByte : kotlin.Comparable<kotlin.UByte> {
    @kotlin.internal.InlineOnly
    public final inline infix fun and(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public open override inline operator fun compareTo(other: kotlin.UByte): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UInt): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.ULong): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UShort): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun dec(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UShort): kotlin.UInt

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UShort): kotlin.UInt

    public open override fun hashCode(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun inc(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun inv(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun or(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rangeTo(other: kotlin.UByte): kotlin.ranges.UIntRange

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toByte(): kotlin.Byte

    @kotlin.internal.InlineOnly
    public final inline fun toDouble(): kotlin.Double

    @kotlin.internal.InlineOnly
    public final inline fun toFloat(): kotlin.Float

    @kotlin.internal.InlineOnly
    public final inline fun toInt(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline fun toLong(): kotlin.Long

    @kotlin.internal.InlineOnly
    public final inline fun toShort(): kotlin.Short

    public open override fun toString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun toUByte(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun toUInt(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toULong(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun toUShort(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun xor(other: kotlin.UByte): kotlin.UByte

    public companion object of UByte {
        public const final val MAX_VALUE: kotlin.UByte = -1.toUByte() { get; }

        public const final val MIN_VALUE: kotlin.UByte = 0.toUByte() { get; }

        public const final val SIZE_BITS: kotlin.Int = 8 { get; }

        public const final val SIZE_BYTES: kotlin.Int = 1 { get; }
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.jvm.JvmInline
public final inline class UByteArray : kotlin.collections.Collection<kotlin.UByte> {
    public constructor UByteArray(size: kotlin.Int)

    public open override val size: kotlin.Int { get; }

    public open override operator fun contains(element: kotlin.UByte): kotlin.Boolean

    public open override fun containsAll(elements: kotlin.collections.Collection<kotlin.UByte>): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final operator fun get(index: kotlin.Int): kotlin.UByte

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.Iterator<kotlin.UByte>

    public final operator fun set(index: kotlin.Int, value: kotlin.UByte): kotlin.Unit

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.jvm.JvmInline
public final inline class UInt : kotlin.Comparable<kotlin.UInt> {
    @kotlin.internal.InlineOnly
    public final inline infix fun and(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UByte): kotlin.Int

    @kotlin.internal.InlineOnly
    public open override inline operator fun compareTo(other: kotlin.UInt): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.ULong): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UShort): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun dec(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UShort): kotlin.UInt

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UShort): kotlin.UInt

    public open override fun hashCode(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun inc(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun inv(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun or(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rangeTo(other: kotlin.UInt): kotlin.ranges.UIntRange

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline infix fun shl(bitCount: kotlin.Int): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline infix fun shr(bitCount: kotlin.Int): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toByte(): kotlin.Byte

    @kotlin.internal.InlineOnly
    public final inline fun toDouble(): kotlin.Double

    @kotlin.internal.InlineOnly
    public final inline fun toFloat(): kotlin.Float

    @kotlin.internal.InlineOnly
    public final inline fun toInt(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline fun toLong(): kotlin.Long

    @kotlin.internal.InlineOnly
    public final inline fun toShort(): kotlin.Short

    public open override fun toString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun toUByte(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun toUInt(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toULong(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun toUShort(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun xor(other: kotlin.UInt): kotlin.UInt

    public companion object of UInt {
        public const final val MAX_VALUE: kotlin.UInt = -1.toUInt() { get; }

        public const final val MIN_VALUE: kotlin.UInt = 0.toUInt() { get; }

        public const final val SIZE_BITS: kotlin.Int = 32 { get; }

        public const final val SIZE_BYTES: kotlin.Int = 4 { get; }
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.jvm.JvmInline
public final inline class UIntArray : kotlin.collections.Collection<kotlin.UInt> {
    public constructor UIntArray(size: kotlin.Int)

    public open override val size: kotlin.Int { get; }

    public open override operator fun contains(element: kotlin.UInt): kotlin.Boolean

    public open override fun containsAll(elements: kotlin.collections.Collection<kotlin.UInt>): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final operator fun get(index: kotlin.Int): kotlin.UInt

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.Iterator<kotlin.UInt>

    public final operator fun set(index: kotlin.Int, value: kotlin.UInt): kotlin.Unit

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.jvm.JvmInline
public final inline class ULong : kotlin.Comparable<kotlin.ULong> {
    @kotlin.internal.InlineOnly
    public final inline infix fun and(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UByte): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UInt): kotlin.Int

    @kotlin.internal.InlineOnly
    public open override inline operator fun compareTo(other: kotlin.ULong): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UShort): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun dec(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UShort): kotlin.ULong

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UShort): kotlin.ULong

    public open override fun hashCode(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun inc(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun inv(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UShort): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun or(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UShort): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rangeTo(other: kotlin.ULong): kotlin.ranges.ULongRange

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UShort): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline infix fun shl(bitCount: kotlin.Int): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline infix fun shr(bitCount: kotlin.Int): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UByte): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UInt): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UShort): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun toByte(): kotlin.Byte

    @kotlin.internal.InlineOnly
    public final inline fun toDouble(): kotlin.Double

    @kotlin.internal.InlineOnly
    public final inline fun toFloat(): kotlin.Float

    @kotlin.internal.InlineOnly
    public final inline fun toInt(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline fun toLong(): kotlin.Long

    @kotlin.internal.InlineOnly
    public final inline fun toShort(): kotlin.Short

    public open override fun toString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun toUByte(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun toUInt(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toULong(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun toUShort(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun xor(other: kotlin.ULong): kotlin.ULong

    public companion object of ULong {
        public const final val MAX_VALUE: kotlin.ULong = -1.toULong() { get; }

        public const final val MIN_VALUE: kotlin.ULong = 0.toULong() { get; }

        public const final val SIZE_BITS: kotlin.Int = 64 { get; }

        public const final val SIZE_BYTES: kotlin.Int = 8 { get; }
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.jvm.JvmInline
public final inline class ULongArray : kotlin.collections.Collection<kotlin.ULong> {
    public constructor ULongArray(size: kotlin.Int)

    public open override val size: kotlin.Int { get; }

    public open override operator fun contains(element: kotlin.ULong): kotlin.Boolean

    public open override fun containsAll(elements: kotlin.collections.Collection<kotlin.ULong>): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final operator fun get(index: kotlin.Int): kotlin.ULong

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.Iterator<kotlin.ULong>

    public final operator fun set(index: kotlin.Int, value: kotlin.ULong): kotlin.Unit

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.jvm.JvmInline
public final inline class UShort : kotlin.Comparable<kotlin.UShort> {
    @kotlin.internal.InlineOnly
    public final inline infix fun and(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UByte): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.UInt): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun compareTo(other: kotlin.ULong): kotlin.Int

    @kotlin.internal.InlineOnly
    public open override inline operator fun compareTo(other: kotlin.UShort): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun dec(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun div(other: kotlin.UShort): kotlin.UInt

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun floorDiv(other: kotlin.UShort): kotlin.UInt

    public open override fun hashCode(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline operator fun inc(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline fun inv(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun minus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UByte): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun mod(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun or(other: kotlin.UShort): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun plus(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rangeTo(other: kotlin.UShort): kotlin.ranges.UIntRange

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun rem(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UByte): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UInt): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.ULong): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline operator fun times(other: kotlin.UShort): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toByte(): kotlin.Byte

    @kotlin.internal.InlineOnly
    public final inline fun toDouble(): kotlin.Double

    @kotlin.internal.InlineOnly
    public final inline fun toFloat(): kotlin.Float

    @kotlin.internal.InlineOnly
    public final inline fun toInt(): kotlin.Int

    @kotlin.internal.InlineOnly
    public final inline fun toLong(): kotlin.Long

    @kotlin.internal.InlineOnly
    public final inline fun toShort(): kotlin.Short

    public open override fun toString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun toUByte(): kotlin.UByte

    @kotlin.internal.InlineOnly
    public final inline fun toUInt(): kotlin.UInt

    @kotlin.internal.InlineOnly
    public final inline fun toULong(): kotlin.ULong

    @kotlin.internal.InlineOnly
    public final inline fun toUShort(): kotlin.UShort

    @kotlin.internal.InlineOnly
    public final inline infix fun xor(other: kotlin.UShort): kotlin.UShort

    public companion object of UShort {
        public const final val MAX_VALUE: kotlin.UShort = -1.toUShort() { get; }

        public const final val MIN_VALUE: kotlin.UShort = 0.toUShort() { get; }

        public const final val SIZE_BITS: kotlin.Int = 16 { get; }

        public const final val SIZE_BYTES: kotlin.Int = 2 { get; }
    }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.jvm.JvmInline
public final inline class UShortArray : kotlin.collections.Collection<kotlin.UShort> {
    public constructor UShortArray(size: kotlin.Int)

    public open override val size: kotlin.Int { get; }

    public open override operator fun contains(element: kotlin.UShort): kotlin.Boolean

    public open override fun containsAll(elements: kotlin.collections.Collection<kotlin.UShort>): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public final operator fun get(index: kotlin.Int): kotlin.UShort

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.Iterator<kotlin.UShort>

    public final operator fun set(index: kotlin.Int, value: kotlin.UShort): kotlin.Unit

    public open override fun toString(): kotlin.String
}

public open class UninitializedPropertyAccessException : kotlin.RuntimeException {
    public constructor UninitializedPropertyAccessException()

    public constructor UninitializedPropertyAccessException(message: kotlin.String?)

    public constructor UninitializedPropertyAccessException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor UninitializedPropertyAccessException(cause: kotlin.Throwable?)
}

public object Unit {
    public open override fun toString(): kotlin.String
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
@kotlin.annotation.MustBeDocumented
public final annotation class UnsafeVariance : kotlin.Annotation {
    public constructor UnsafeVariance()
}

public open class UnsupportedOperationException : kotlin.RuntimeException {
    public constructor UnsupportedOperationException()

    public constructor UnsupportedOperationException(message: kotlin.String?)

    public constructor UnsupportedOperationException(message: kotlin.String?, cause: kotlin.Throwable?)

    public constructor UnsupportedOperationException(cause: kotlin.Throwable?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.SOURCE)
@kotlin.SinceKotlin(version = "1.2")
@kotlin.DeprecatedSinceKotlin(errorSince = "1.6", warningSince = "1.4")
@kotlin.Deprecated(message = "Please use OptIn instead.", replaceWith = kotlin.ReplaceWith(expression = "OptIn(*markerClass)", imports = {"kotlin.OptIn"}))
public final annotation class UseExperimental : kotlin.Annotation {
    public constructor UseExperimental(vararg markerClass: kotlin.reflect.KClass<out kotlin.Annotation>)

    public final val markerClass: kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>> { get; }
}