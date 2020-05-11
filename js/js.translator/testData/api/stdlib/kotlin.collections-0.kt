package kotlin.collections

public val </*0*/ T> kotlin.Array<out T>.indices: kotlin.ranges.IntRange
    public fun kotlin.Array<out T>.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.BooleanArray.indices: kotlin.ranges.IntRange
    public fun kotlin.BooleanArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.ByteArray.indices: kotlin.ranges.IntRange
    public fun kotlin.ByteArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.CharArray.indices: kotlin.ranges.IntRange
    public fun kotlin.CharArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.DoubleArray.indices: kotlin.ranges.IntRange
    public fun kotlin.DoubleArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.FloatArray.indices: kotlin.ranges.IntRange
    public fun kotlin.FloatArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.IntArray.indices: kotlin.ranges.IntRange
    public fun kotlin.IntArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.LongArray.indices: kotlin.ranges.IntRange
    public fun kotlin.LongArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.ShortArray.indices: kotlin.ranges.IntRange
    public fun kotlin.ShortArray.<get-indices>(): kotlin.ranges.IntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UByteArray.indices: kotlin.ranges.IntRange
    public inline fun kotlin.UByteArray.<get-indices>(): kotlin.ranges.IntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UIntArray.indices: kotlin.ranges.IntRange
    public inline fun kotlin.UIntArray.<get-indices>(): kotlin.ranges.IntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.ULongArray.indices: kotlin.ranges.IntRange
    public inline fun kotlin.ULongArray.<get-indices>(): kotlin.ranges.IntRange
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UShortArray.indices: kotlin.ranges.IntRange
    public inline fun kotlin.UShortArray.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.collections.Collection<*>.indices: kotlin.ranges.IntRange
    public fun kotlin.collections.Collection<*>.<get-indices>(): kotlin.ranges.IntRange
public val </*0*/ T> kotlin.Array<out T>.lastIndex: kotlin.Int
    public fun kotlin.Array<out T>.<get-lastIndex>(): kotlin.Int
public val kotlin.BooleanArray.lastIndex: kotlin.Int
    public fun kotlin.BooleanArray.<get-lastIndex>(): kotlin.Int
public val kotlin.ByteArray.lastIndex: kotlin.Int
    public fun kotlin.ByteArray.<get-lastIndex>(): kotlin.Int
public val kotlin.CharArray.lastIndex: kotlin.Int
    public fun kotlin.CharArray.<get-lastIndex>(): kotlin.Int
public val kotlin.DoubleArray.lastIndex: kotlin.Int
    public fun kotlin.DoubleArray.<get-lastIndex>(): kotlin.Int
public val kotlin.FloatArray.lastIndex: kotlin.Int
    public fun kotlin.FloatArray.<get-lastIndex>(): kotlin.Int
public val kotlin.IntArray.lastIndex: kotlin.Int
    public fun kotlin.IntArray.<get-lastIndex>(): kotlin.Int
public val kotlin.LongArray.lastIndex: kotlin.Int
    public fun kotlin.LongArray.<get-lastIndex>(): kotlin.Int
public val kotlin.ShortArray.lastIndex: kotlin.Int
    public fun kotlin.ShortArray.<get-lastIndex>(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UByteArray.lastIndex: kotlin.Int
    public inline fun kotlin.UByteArray.<get-lastIndex>(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UIntArray.lastIndex: kotlin.Int
    public inline fun kotlin.UIntArray.<get-lastIndex>(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.ULongArray.lastIndex: kotlin.Int
    public inline fun kotlin.ULongArray.<get-lastIndex>(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public val kotlin.UShortArray.lastIndex: kotlin.Int
    public inline fun kotlin.UShortArray.<get-lastIndex>(): kotlin.Int
public val </*0*/ T> kotlin.collections.List<T>.lastIndex: kotlin.Int
    public fun kotlin.collections.List<T>.<get-lastIndex>(): kotlin.Int
@kotlin.internal.InlineOnly public inline fun </*0*/ T> Iterable(/*0*/ crossinline iterator: () -> kotlin.collections.Iterator<T>): kotlin.collections.Iterable<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> List(/*0*/ size: kotlin.Int, /*1*/ init: (index: kotlin.Int) -> T): kotlin.collections.List<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> MutableList(/*0*/ size: kotlin.Int, /*1*/ init: (index: kotlin.Int) -> T): kotlin.collections.MutableList<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> arrayListOf(): kotlin.collections.ArrayList<T>
public fun </*0*/ T> arrayListOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.ArrayList<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ E> buildList(/*0*/ capacity: kotlin.Int, /*1*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableList<E>.() -> kotlin.Unit): kotlin.collections.List<E>
    CallsInPlace(builderAction, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ E> buildList(/*0*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableList<E>.() -> kotlin.Unit): kotlin.collections.List<E>
    CallsInPlace(builderAction, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> buildMap(/*0*/ capacity: kotlin.Int, /*1*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableMap<K, V>.() -> kotlin.Unit): kotlin.collections.Map<K, V>
    CallsInPlace(builderAction, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> buildMap(/*0*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableMap<K, V>.() -> kotlin.Unit): kotlin.collections.Map<K, V>
    CallsInPlace(builderAction, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ E> buildSet(/*0*/ capacity: kotlin.Int, /*1*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableSet<E>.() -> kotlin.Unit): kotlin.collections.Set<E>
    CallsInPlace(builderAction, EXACTLY_ONCE)

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ E> buildSet(/*0*/ @kotlin.BuilderInference builderAction: kotlin.collections.MutableSet<E>.() -> kotlin.Unit): kotlin.collections.Set<E>
    CallsInPlace(builderAction, EXACTLY_ONCE)

public fun </*0*/ T> emptyList(): kotlin.collections.List<T>
public fun </*0*/ K, /*1*/ V> emptyMap(): kotlin.collections.Map<K, V>
public fun </*0*/ T> emptySet(): kotlin.collections.Set<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> hashMapOf(): kotlin.collections.HashMap<K, V>
public fun </*0*/ K, /*1*/ V> hashMapOf(/*0*/ vararg pairs: kotlin.Pair<K, V> /*kotlin.Array<out kotlin.Pair<K, V>>*/): kotlin.collections.HashMap<K, V>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> hashSetOf(): kotlin.collections.HashSet<T>
public fun </*0*/ T> hashSetOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.HashSet<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> linkedMapOf(): kotlin.collections.LinkedHashMap<K, V>
public fun </*0*/ K, /*1*/ V> linkedMapOf(/*0*/ vararg pairs: kotlin.Pair<K, V> /*kotlin.Array<out kotlin.Pair<K, V>>*/): kotlin.collections.LinkedHashMap<K, V>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> linkedSetOf(): kotlin.collections.LinkedHashSet<T>
public fun </*0*/ T> linkedSetOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.LinkedHashSet<T>
public fun </*0*/ V> linkedStringMapOf(/*0*/ vararg pairs: kotlin.Pair<kotlin.String, V> /*kotlin.Array<out kotlin.Pair<kotlin.String, V>>*/): kotlin.collections.LinkedHashMap<kotlin.String, V>
public fun linkedStringSetOf(/*0*/ vararg elements: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.collections.LinkedHashSet<kotlin.String>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> listOf(): kotlin.collections.List<T>
public fun </*0*/ T> listOf(/*0*/ element: T): kotlin.collections.List<T>
public fun </*0*/ T> listOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.List<T>
public fun </*0*/ T : kotlin.Any> listOfNotNull(/*0*/ element: T?): kotlin.collections.List<T>
public fun </*0*/ T : kotlin.Any> listOfNotNull(/*0*/ vararg elements: T? /*kotlin.Array<out T?>*/): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> mapOf(): kotlin.collections.Map<K, V>
public fun </*0*/ K, /*1*/ V> mapOf(/*0*/ vararg pairs: kotlin.Pair<K, V> /*kotlin.Array<out kotlin.Pair<K, V>>*/): kotlin.collections.Map<K, V>
public fun </*0*/ K, /*1*/ V> mapOf(/*0*/ pair: kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> mutableListOf(): kotlin.collections.MutableList<T>
public fun </*0*/ T> mutableListOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.MutableList<T>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> mutableMapOf(): kotlin.collections.MutableMap<K, V>
public fun </*0*/ K, /*1*/ V> mutableMapOf(/*0*/ vararg pairs: kotlin.Pair<K, V> /*kotlin.Array<out kotlin.Pair<K, V>>*/): kotlin.collections.MutableMap<K, V>
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun </*0*/ T> mutableSetOf(): kotlin.collections.MutableSet<T>
public fun </*0*/ T> mutableSetOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.MutableSet<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> setOf(): kotlin.collections.Set<T>
public fun </*0*/ T> setOf(/*0*/ element: T): kotlin.collections.Set<T>
public fun </*0*/ T> setOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.collections.Set<T>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T : kotlin.Any> setOfNotNull(/*0*/ element: T?): kotlin.collections.Set<T>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T : kotlin.Any> setOfNotNull(/*0*/ vararg elements: T? /*kotlin.Array<out T?>*/): kotlin.collections.Set<T>
public fun </*0*/ V> stringMapOf(/*0*/ vararg pairs: kotlin.Pair<kotlin.String, V> /*kotlin.Array<out kotlin.Pair<kotlin.String, V>>*/): kotlin.collections.HashMap<kotlin.String, V>
public fun stringSetOf(/*0*/ vararg elements: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.collections.HashSet<kotlin.String>
public fun </*0*/ T> kotlin.collections.MutableCollection<in T>.addAll(/*0*/ elements: kotlin.Array<out T>): kotlin.Boolean
public fun </*0*/ T> kotlin.collections.MutableCollection<in T>.addAll(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.Boolean
public fun </*0*/ T> kotlin.collections.MutableCollection<in T>.addAll(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R> kotlin.collections.Grouping<T, K>.aggregate(/*0*/ operation: (key: K, accumulator: R?, element: T, first: kotlin.Boolean) -> R): kotlin.collections.Map<K, R>
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R, /*3*/ M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.aggregateTo(/*0*/ destination: M, /*1*/ operation: (key: K, accumulator: R?, element: T, first: kotlin.Boolean) -> R): M
public inline fun </*0*/ T> kotlin.Array<out T>.all(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.BooleanArray.all(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.ByteArray.all(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.CharArray.all(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.DoubleArray.all(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.FloatArray.all(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.IntArray.all(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.LongArray.all(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.ShortArray.all(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.all(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.all(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.all(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.all(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.all(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.all(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T> kotlin.Array<out T>.any(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.Array<out T>.any(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.BooleanArray.any(): kotlin.Boolean
public inline fun kotlin.BooleanArray.any(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ByteArray.any(): kotlin.Boolean
public inline fun kotlin.ByteArray.any(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.CharArray.any(): kotlin.Boolean
public inline fun kotlin.CharArray.any(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.DoubleArray.any(): kotlin.Boolean
public inline fun kotlin.DoubleArray.any(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.FloatArray.any(): kotlin.Boolean
public inline fun kotlin.FloatArray.any(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.IntArray.any(): kotlin.Boolean
public inline fun kotlin.IntArray.any(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.LongArray.any(): kotlin.Boolean
public inline fun kotlin.LongArray.any(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ShortArray.any(): kotlin.Boolean
public inline fun kotlin.ShortArray.any(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.any(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.any(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.any(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.any(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.any(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.any(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.any(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.any(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T> kotlin.collections.Iterable<T>.any(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.any(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.any(): kotlin.Boolean
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.any(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.asByteArray(): kotlin.ByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.asIntArray(): kotlin.IntArray
public fun </*0*/ T> kotlin.Array<out T>.asIterable(): kotlin.collections.Iterable<T>
public fun kotlin.BooleanArray.asIterable(): kotlin.collections.Iterable<kotlin.Boolean>
public fun kotlin.ByteArray.asIterable(): kotlin.collections.Iterable<kotlin.Byte>
public fun kotlin.CharArray.asIterable(): kotlin.collections.Iterable<kotlin.Char>
public fun kotlin.DoubleArray.asIterable(): kotlin.collections.Iterable<kotlin.Double>
public fun kotlin.FloatArray.asIterable(): kotlin.collections.Iterable<kotlin.Float>
public fun kotlin.IntArray.asIterable(): kotlin.collections.Iterable<kotlin.Int>
public fun kotlin.LongArray.asIterable(): kotlin.collections.Iterable<kotlin.Long>
public fun kotlin.ShortArray.asIterable(): kotlin.collections.Iterable<kotlin.Short>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.asIterable(): kotlin.collections.Iterable<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.asIterable(): kotlin.collections.Iterable<kotlin.collections.Map.Entry<K, V>>
public fun </*0*/ T> kotlin.Array<out T>.asList(): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.asList(): kotlin.collections.List<kotlin.Boolean>
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.asList(): kotlin.collections.List<kotlin.Byte>
public fun kotlin.CharArray.asList(): kotlin.collections.List<kotlin.Char>
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.asList(): kotlin.collections.List<kotlin.Double>
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.asList(): kotlin.collections.List<kotlin.Float>
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.asList(): kotlin.collections.List<kotlin.Int>
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.asList(): kotlin.collections.List<kotlin.Long>
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.asList(): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.asList(): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.asList(): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.asList(): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.asList(): kotlin.collections.List<kotlin.UShort>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.asLongArray(): kotlin.LongArray
public fun </*0*/ T> kotlin.collections.List<T>.asReversed(): kotlin.collections.List<T>
@kotlin.jvm.JvmName(name = "asReversedMutable") public fun </*0*/ T> kotlin.collections.MutableList<T>.asReversed(): kotlin.collections.MutableList<T>
public fun </*0*/ T> kotlin.Array<out T>.asSequence(): kotlin.sequences.Sequence<T>
public fun kotlin.BooleanArray.asSequence(): kotlin.sequences.Sequence<kotlin.Boolean>
public fun kotlin.ByteArray.asSequence(): kotlin.sequences.Sequence<kotlin.Byte>
public fun kotlin.CharArray.asSequence(): kotlin.sequences.Sequence<kotlin.Char>
public fun kotlin.DoubleArray.asSequence(): kotlin.sequences.Sequence<kotlin.Double>
public fun kotlin.FloatArray.asSequence(): kotlin.sequences.Sequence<kotlin.Float>
public fun kotlin.IntArray.asSequence(): kotlin.sequences.Sequence<kotlin.Int>
public fun kotlin.LongArray.asSequence(): kotlin.sequences.Sequence<kotlin.Long>
public fun kotlin.ShortArray.asSequence(): kotlin.sequences.Sequence<kotlin.Short>
public fun </*0*/ T> kotlin.collections.Iterable<T>.asSequence(): kotlin.sequences.Sequence<T>
public fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.asSequence(): kotlin.sequences.Sequence<kotlin.collections.Map.Entry<K, V>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.asShortArray(): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.asUByteArray(): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.asUIntArray(): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.asULongArray(): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.asUShortArray(): kotlin.UShortArray
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.Array<out T>.associate(/*0*/ transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.BooleanArray.associate(/*0*/ transform: (kotlin.Boolean) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.ByteArray.associate(/*0*/ transform: (kotlin.Byte) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharArray.associate(/*0*/ transform: (kotlin.Char) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.DoubleArray.associate(/*0*/ transform: (kotlin.Double) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.FloatArray.associate(/*0*/ transform: (kotlin.Float) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.IntArray.associate(/*0*/ transform: (kotlin.Int) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.LongArray.associate(/*0*/ transform: (kotlin.Long) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ V> kotlin.ShortArray.associate(/*0*/ transform: (kotlin.Short) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.collections.Iterable<T>.associate(/*0*/ transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K> kotlin.Array<out T>.associateBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, T>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.Array<out T>.associateBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.BooleanArray.associateBy(/*0*/ keySelector: (kotlin.Boolean) -> K): kotlin.collections.Map<K, kotlin.Boolean>
public inline fun </*0*/ K, /*1*/ V> kotlin.BooleanArray.associateBy(/*0*/ keySelector: (kotlin.Boolean) -> K, /*1*/ valueTransform: (kotlin.Boolean) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.ByteArray.associateBy(/*0*/ keySelector: (kotlin.Byte) -> K): kotlin.collections.Map<K, kotlin.Byte>
public inline fun </*0*/ K, /*1*/ V> kotlin.ByteArray.associateBy(/*0*/ keySelector: (kotlin.Byte) -> K, /*1*/ valueTransform: (kotlin.Byte) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.CharArray.associateBy(/*0*/ keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.Char>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharArray.associateBy(/*0*/ keySelector: (kotlin.Char) -> K, /*1*/ valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.DoubleArray.associateBy(/*0*/ keySelector: (kotlin.Double) -> K): kotlin.collections.Map<K, kotlin.Double>
public inline fun </*0*/ K, /*1*/ V> kotlin.DoubleArray.associateBy(/*0*/ keySelector: (kotlin.Double) -> K, /*1*/ valueTransform: (kotlin.Double) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.FloatArray.associateBy(/*0*/ keySelector: (kotlin.Float) -> K): kotlin.collections.Map<K, kotlin.Float>
public inline fun </*0*/ K, /*1*/ V> kotlin.FloatArray.associateBy(/*0*/ keySelector: (kotlin.Float) -> K, /*1*/ valueTransform: (kotlin.Float) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.IntArray.associateBy(/*0*/ keySelector: (kotlin.Int) -> K): kotlin.collections.Map<K, kotlin.Int>
public inline fun </*0*/ K, /*1*/ V> kotlin.IntArray.associateBy(/*0*/ keySelector: (kotlin.Int) -> K, /*1*/ valueTransform: (kotlin.Int) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.LongArray.associateBy(/*0*/ keySelector: (kotlin.Long) -> K): kotlin.collections.Map<K, kotlin.Long>
public inline fun </*0*/ K, /*1*/ V> kotlin.LongArray.associateBy(/*0*/ keySelector: (kotlin.Long) -> K, /*1*/ valueTransform: (kotlin.Long) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.ShortArray.associateBy(/*0*/ keySelector: (kotlin.Short) -> K): kotlin.collections.Map<K, kotlin.Short>
public inline fun </*0*/ K, /*1*/ V> kotlin.ShortArray.associateBy(/*0*/ keySelector: (kotlin.Short) -> K, /*1*/ valueTransform: (kotlin.Short) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K> kotlin.collections.Iterable<T>.associateBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, T>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.collections.Iterable<T>.associateBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, in T>> kotlin.Array<out T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Boolean>> kotlin.BooleanArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Boolean) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.BooleanArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Boolean) -> K, /*2*/ valueTransform: (kotlin.Boolean) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Byte>> kotlin.ByteArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Byte) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.ByteArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Byte) -> K, /*2*/ valueTransform: (kotlin.Byte) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Char>> kotlin.CharArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K, /*2*/ valueTransform: (kotlin.Char) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Double>> kotlin.DoubleArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Double) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.DoubleArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Double) -> K, /*2*/ valueTransform: (kotlin.Double) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Float>> kotlin.FloatArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Float) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.FloatArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Float) -> K, /*2*/ valueTransform: (kotlin.Float) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Int>> kotlin.IntArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Int) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.IntArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Int) -> K, /*2*/ valueTransform: (kotlin.Int) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Long>> kotlin.LongArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Long) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.LongArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Long) -> K, /*2*/ valueTransform: (kotlin.Long) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Short>> kotlin.ShortArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Short) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.ShortArray.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Short) -> K, /*2*/ valueTransform: (kotlin.Short) -> V): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, in T>> kotlin.collections.Iterable<T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out T>.associateTo(/*0*/ destination: M, /*1*/ transform: (T) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.BooleanArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Boolean) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.ByteArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Byte) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Char) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.DoubleArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Double) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.FloatArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Float) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.IntArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Int) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.LongArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Long) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.ShortArray.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Short) -> kotlin.Pair<K, V>): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<T>.associateTo(/*0*/ destination: M, /*1*/ transform: (T) -> kotlin.Pair<K, V>): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ K, /*1*/ V> kotlin.Array<out K>.associateWith(/*0*/ valueSelector: (K) -> V): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.BooleanArray.associateWith(/*0*/ valueSelector: (kotlin.Boolean) -> V): kotlin.collections.Map<kotlin.Boolean, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.ByteArray.associateWith(/*0*/ valueSelector: (kotlin.Byte) -> V): kotlin.collections.Map<kotlin.Byte, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.CharArray.associateWith(/*0*/ valueSelector: (kotlin.Char) -> V): kotlin.collections.Map<kotlin.Char, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.DoubleArray.associateWith(/*0*/ valueSelector: (kotlin.Double) -> V): kotlin.collections.Map<kotlin.Double, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.FloatArray.associateWith(/*0*/ valueSelector: (kotlin.Float) -> V): kotlin.collections.Map<kotlin.Float, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.IntArray.associateWith(/*0*/ valueSelector: (kotlin.Int) -> V): kotlin.collections.Map<kotlin.Int, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.LongArray.associateWith(/*0*/ valueSelector: (kotlin.Long) -> V): kotlin.collections.Map<kotlin.Long, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.ShortArray.associateWith(/*0*/ valueSelector: (kotlin.Short) -> V): kotlin.collections.Map<kotlin.Short, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.UByteArray.associateWith(/*0*/ valueSelector: (kotlin.UByte) -> V): kotlin.collections.Map<kotlin.UByte, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.UIntArray.associateWith(/*0*/ valueSelector: (kotlin.UInt) -> V): kotlin.collections.Map<kotlin.UInt, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.ULongArray.associateWith(/*0*/ valueSelector: (kotlin.ULong) -> V): kotlin.collections.Map<kotlin.ULong, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.UShortArray.associateWith(/*0*/ valueSelector: (kotlin.UShort) -> V): kotlin.collections.Map<kotlin.UShort, V>
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Iterable<K>.associateWith(/*0*/ valueSelector: (K) -> V): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out K>.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (K) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Boolean, in V>> kotlin.BooleanArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Boolean) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Byte, in V>> kotlin.ByteArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Byte) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Char, in V>> kotlin.CharArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Char) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Double, in V>> kotlin.DoubleArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Double) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Float, in V>> kotlin.FloatArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Float) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Int, in V>> kotlin.IntArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Int) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Long, in V>> kotlin.LongArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Long) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Short, in V>> kotlin.ShortArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Short) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.UByte, in V>> kotlin.UByteArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.UByte) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.UInt, in V>> kotlin.UIntArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.UInt) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.ULong, in V>> kotlin.ULongArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.ULong) -> V): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.UShort, in V>> kotlin.UShortArray.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.UShort) -> V): M
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<K>.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (K) -> V): M
@kotlin.jvm.JvmName(name = "averageOfByte") public fun kotlin.Array<out kotlin.Byte>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfDouble") public fun kotlin.Array<out kotlin.Double>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfFloat") public fun kotlin.Array<out kotlin.Float>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfInt") public fun kotlin.Array<out kotlin.Int>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfLong") public fun kotlin.Array<out kotlin.Long>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfShort") public fun kotlin.Array<out kotlin.Short>.average(): kotlin.Double
public fun kotlin.ByteArray.average(): kotlin.Double
public fun kotlin.DoubleArray.average(): kotlin.Double
public fun kotlin.FloatArray.average(): kotlin.Double
public fun kotlin.IntArray.average(): kotlin.Double
public fun kotlin.LongArray.average(): kotlin.Double
public fun kotlin.ShortArray.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfByte") public fun kotlin.collections.Iterable<kotlin.Byte>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfDouble") public fun kotlin.collections.Iterable<kotlin.Double>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfFloat") public fun kotlin.collections.Iterable<kotlin.Float>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfInt") public fun kotlin.collections.Iterable<kotlin.Int>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfLong") public fun kotlin.collections.Iterable<kotlin.Long>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfShort") public fun kotlin.collections.Iterable<kotlin.Short>.average(): kotlin.Double
public fun </*0*/ T> kotlin.collections.List<T>.binarySearch(/*0*/ element: T, /*1*/ comparator: kotlin.Comparator<in T>, /*2*/ fromIndex: kotlin.Int = ..., /*3*/ toIndex: kotlin.Int = ...): kotlin.Int
public fun </*0*/ T> kotlin.collections.List<T>.binarySearch(/*0*/ fromIndex: kotlin.Int = ..., /*1*/ toIndex: kotlin.Int = ..., /*2*/ comparison: (T) -> kotlin.Int): kotlin.Int
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.collections.List<T?>.binarySearch(/*0*/ element: T?, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Int
public inline fun </*0*/ T, /*1*/ K : kotlin.Comparable<K>> kotlin.collections.List<T>.binarySearchBy(/*0*/ key: K?, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ..., /*3*/ crossinline selector: (T) -> K?): kotlin.Int
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T> kotlin.collections.Iterable<T>.chunked(/*0*/ size: kotlin.Int): kotlin.collections.List<kotlin.collections.List<T>>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.chunked(/*0*/ size: kotlin.Int, /*1*/ transform: (kotlin.collections.List<T>) -> R): kotlin.collections.List<R>
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Array<out T>.component1(): T
@kotlin.internal.InlineOnly public inline operator fun kotlin.BooleanArray.component1(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.ByteArray.component1(): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharArray.component1(): kotlin.Char
@kotlin.internal.InlineOnly public inline operator fun kotlin.DoubleArray.component1(): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun kotlin.FloatArray.component1(): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun kotlin.IntArray.component1(): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun kotlin.LongArray.component1(): kotlin.Long
@kotlin.internal.InlineOnly public inline operator fun kotlin.ShortArray.component1(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.component1(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.component1(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.component1(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.component1(): kotlin.UShort
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.List<T>.component1(): T
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map.Entry<K, V>.component1(): K
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Array<out T>.component2(): T
@kotlin.internal.InlineOnly public inline operator fun kotlin.BooleanArray.component2(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.ByteArray.component2(): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharArray.component2(): kotlin.Char
@kotlin.internal.InlineOnly public inline operator fun kotlin.DoubleArray.component2(): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun kotlin.FloatArray.component2(): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun kotlin.IntArray.component2(): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun kotlin.LongArray.component2(): kotlin.Long
@kotlin.internal.InlineOnly public inline operator fun kotlin.ShortArray.component2(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.component2(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.component2(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.component2(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.component2(): kotlin.UShort
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.List<T>.component2(): T
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map.Entry<K, V>.component2(): V
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Array<out T>.component3(): T
@kotlin.internal.InlineOnly public inline operator fun kotlin.BooleanArray.component3(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.ByteArray.component3(): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharArray.component3(): kotlin.Char
@kotlin.internal.InlineOnly public inline operator fun kotlin.DoubleArray.component3(): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun kotlin.FloatArray.component3(): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun kotlin.IntArray.component3(): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun kotlin.LongArray.component3(): kotlin.Long
@kotlin.internal.InlineOnly public inline operator fun kotlin.ShortArray.component3(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.component3(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.component3(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.component3(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.component3(): kotlin.UShort
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.List<T>.component3(): T
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Array<out T>.component4(): T
@kotlin.internal.InlineOnly public inline operator fun kotlin.BooleanArray.component4(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.ByteArray.component4(): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharArray.component4(): kotlin.Char
@kotlin.internal.InlineOnly public inline operator fun kotlin.DoubleArray.component4(): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun kotlin.FloatArray.component4(): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun kotlin.IntArray.component4(): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun kotlin.LongArray.component4(): kotlin.Long
@kotlin.internal.InlineOnly public inline operator fun kotlin.ShortArray.component4(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.component4(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.component4(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.component4(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.component4(): kotlin.UShort
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.List<T>.component4(): T
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.Array<out T>.component5(): T
@kotlin.internal.InlineOnly public inline operator fun kotlin.BooleanArray.component5(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.ByteArray.component5(): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharArray.component5(): kotlin.Char
@kotlin.internal.InlineOnly public inline operator fun kotlin.DoubleArray.component5(): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun kotlin.FloatArray.component5(): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun kotlin.IntArray.component5(): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun kotlin.LongArray.component5(): kotlin.Long
@kotlin.internal.InlineOnly public inline operator fun kotlin.ShortArray.component5(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.component5(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.component5(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.component5(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.component5(): kotlin.UShort
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.List<T>.component5(): T
public operator fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.Array<out T>.contains(/*0*/ element: T): kotlin.Boolean
public operator fun kotlin.BooleanArray.contains(/*0*/ element: kotlin.Boolean): kotlin.Boolean
public operator fun kotlin.ByteArray.contains(/*0*/ element: kotlin.Byte): kotlin.Boolean
public operator fun kotlin.CharArray.contains(/*0*/ element: kotlin.Char): kotlin.Boolean
public operator fun kotlin.DoubleArray.contains(/*0*/ element: kotlin.Double): kotlin.Boolean
public operator fun kotlin.FloatArray.contains(/*0*/ element: kotlin.Float): kotlin.Boolean
public operator fun kotlin.IntArray.contains(/*0*/ element: kotlin.Int): kotlin.Boolean
public operator fun kotlin.LongArray.contains(/*0*/ element: kotlin.Long): kotlin.Boolean
public operator fun kotlin.ShortArray.contains(/*0*/ element: kotlin.Short): kotlin.Boolean
public operator fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.Iterable<T>.contains(/*0*/ element: T): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun </*0*/ @kotlin.internal.OnlyInputTypes K, /*1*/ V> kotlin.collections.Map<out K, V>.contains(/*0*/ key: K): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.Collection<T>.containsAll(/*0*/ elements: kotlin.collections.Collection<T>): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ @kotlin.internal.OnlyInputTypes K> kotlin.collections.Map<out K, *>.containsKey(/*0*/ key: K): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ @kotlin.internal.OnlyInputTypes V> kotlin.collections.Map<K, V>.containsValue(/*0*/ value: V): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun </*0*/ T> kotlin.Array<out T>.contentDeepEquals(/*0*/ other: kotlin.Array<out T>): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayDeepEquals") public infix fun </*0*/ T> kotlin.Array<out T>?.contentDeepEquals(/*0*/ other: kotlin.Array<out T>?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T> kotlin.Array<out T>.contentDeepHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayDeepHashCode") public fun </*0*/ T> kotlin.Array<out T>?.contentDeepHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T> kotlin.Array<out T>.contentDeepToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayDeepToString") public fun </*0*/ T> kotlin.Array<out T>?.contentDeepToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun </*0*/ T> kotlin.Array<out T>.contentEquals(/*0*/ other: kotlin.Array<out T>): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun </*0*/ T> kotlin.Array<out T>?.contentEquals(/*0*/ other: kotlin.Array<out T>?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.BooleanArray.contentEquals(/*0*/ other: kotlin.BooleanArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.BooleanArray?.contentEquals(/*0*/ other: kotlin.BooleanArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.ByteArray.contentEquals(/*0*/ other: kotlin.ByteArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.ByteArray?.contentEquals(/*0*/ other: kotlin.ByteArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.CharArray.contentEquals(/*0*/ other: kotlin.CharArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.CharArray?.contentEquals(/*0*/ other: kotlin.CharArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.DoubleArray.contentEquals(/*0*/ other: kotlin.DoubleArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.DoubleArray?.contentEquals(/*0*/ other: kotlin.DoubleArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.FloatArray.contentEquals(/*0*/ other: kotlin.FloatArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.FloatArray?.contentEquals(/*0*/ other: kotlin.FloatArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.IntArray.contentEquals(/*0*/ other: kotlin.IntArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.IntArray?.contentEquals(/*0*/ other: kotlin.IntArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.LongArray.contentEquals(/*0*/ other: kotlin.LongArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.LongArray?.contentEquals(/*0*/ other: kotlin.LongArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public infix fun kotlin.ShortArray.contentEquals(/*0*/ other: kotlin.ShortArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayEquals") public infix fun kotlin.ShortArray?.contentEquals(/*0*/ other: kotlin.ShortArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UByteArray.contentEquals(/*0*/ other: kotlin.UByteArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UByteArray?.contentEquals(/*0*/ other: kotlin.UByteArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UIntArray.contentEquals(/*0*/ other: kotlin.UIntArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UIntArray?.contentEquals(/*0*/ other: kotlin.UIntArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ULongArray.contentEquals(/*0*/ other: kotlin.ULongArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.ULongArray?.contentEquals(/*0*/ other: kotlin.ULongArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UShortArray.contentEquals(/*0*/ other: kotlin.UShortArray): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public infix fun kotlin.UShortArray?.contentEquals(/*0*/ other: kotlin.UShortArray?): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T> kotlin.Array<out T>.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun </*0*/ T> kotlin.Array<out T>?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.BooleanArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.BooleanArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.ByteArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.ByteArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.CharArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.CharArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.DoubleArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.DoubleArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.FloatArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.FloatArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.IntArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.IntArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.LongArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.LongArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.ShortArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayHashCode") public fun kotlin.ShortArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray?.contentHashCode(): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T> kotlin.Array<out T>.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun </*0*/ T> kotlin.Array<out T>?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.BooleanArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.BooleanArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.ByteArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.ByteArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.CharArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.CharArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.DoubleArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.DoubleArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.FloatArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.FloatArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.IntArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.IntArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.LongArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.LongArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.LowPriorityInOverloadResolution public fun kotlin.ShortArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.js.library(name = "arrayToString") public fun kotlin.ShortArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray?.contentToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.copyInto(/*0*/ destination: kotlin.Array<T>, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.Array<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.copyInto(/*0*/ destination: kotlin.BooleanArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.BooleanArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.copyInto(/*0*/ destination: kotlin.ByteArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.ByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.copyInto(/*0*/ destination: kotlin.CharArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.CharArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.copyInto(/*0*/ destination: kotlin.DoubleArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.DoubleArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.copyInto(/*0*/ destination: kotlin.FloatArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.FloatArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.copyInto(/*0*/ destination: kotlin.IntArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.IntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.copyInto(/*0*/ destination: kotlin.LongArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.LongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.copyInto(/*0*/ destination: kotlin.ShortArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.copyInto(/*0*/ destination: kotlin.UByteArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.copyInto(/*0*/ destination: kotlin.UIntArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.copyInto(/*0*/ destination: kotlin.ULongArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.copyInto(/*0*/ destination: kotlin.UShortArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.UShortArray
public inline fun </*0*/ T> kotlin.Array<out T>.copyOf(): kotlin.Array<T>
public fun </*0*/ T> kotlin.Array<out T>.copyOf(/*0*/ newSize: kotlin.Int): kotlin.Array<T?>
public fun kotlin.BooleanArray.copyOf(): kotlin.BooleanArray
public fun kotlin.BooleanArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.BooleanArray
public inline fun kotlin.ByteArray.copyOf(): kotlin.ByteArray
public fun kotlin.ByteArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.ByteArray
public fun kotlin.CharArray.copyOf(): kotlin.CharArray
public fun kotlin.CharArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.CharArray
public inline fun kotlin.DoubleArray.copyOf(): kotlin.DoubleArray
public fun kotlin.DoubleArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.DoubleArray
public inline fun kotlin.FloatArray.copyOf(): kotlin.FloatArray
public fun kotlin.FloatArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.FloatArray
public inline fun kotlin.IntArray.copyOf(): kotlin.IntArray
public fun kotlin.IntArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.IntArray
public fun kotlin.LongArray.copyOf(): kotlin.LongArray
public fun kotlin.LongArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.LongArray
public inline fun kotlin.ShortArray.copyOf(): kotlin.ShortArray
public fun kotlin.ShortArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.copyOf(): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.copyOf(): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.copyOf(): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.copyOf(): kotlin.UShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.copyOf(/*0*/ newSize: kotlin.Int): kotlin.UShortArray
public fun </*0*/ T> kotlin.Array<out T>.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.Array<T>
public fun kotlin.BooleanArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.BooleanArray
public fun kotlin.ByteArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.ByteArray
public fun kotlin.CharArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.CharArray
public fun kotlin.DoubleArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.DoubleArray
public fun kotlin.FloatArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.FloatArray
public fun kotlin.IntArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.IntArray
public fun kotlin.LongArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.LongArray
public fun kotlin.ShortArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.copyOfRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.UShortArray
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.count(): kotlin.Int
public inline fun </*0*/ T> kotlin.Array<out T>.count(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.count(): kotlin.Int
public inline fun kotlin.BooleanArray.count(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.count(): kotlin.Int
public inline fun kotlin.ByteArray.count(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.count(): kotlin.Int
public inline fun kotlin.CharArray.count(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.count(): kotlin.Int
public inline fun kotlin.DoubleArray.count(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.count(): kotlin.Int
public inline fun kotlin.FloatArray.count(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.count(): kotlin.Int
public inline fun kotlin.IntArray.count(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.count(): kotlin.Int
public inline fun kotlin.LongArray.count(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.count(): kotlin.Int
public inline fun kotlin.ShortArray.count(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.count(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.count(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.count(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.count(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>.count(): kotlin.Int
public fun </*0*/ T> kotlin.collections.Iterable<T>.count(): kotlin.Int
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.count(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.count(): kotlin.Int
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.count(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Int
public fun </*0*/ T> kotlin.Array<out T>.distinct(): kotlin.collections.List<T>
public fun kotlin.BooleanArray.distinct(): kotlin.collections.List<kotlin.Boolean>
public fun kotlin.ByteArray.distinct(): kotlin.collections.List<kotlin.Byte>
public fun kotlin.CharArray.distinct(): kotlin.collections.List<kotlin.Char>
public fun kotlin.DoubleArray.distinct(): kotlin.collections.List<kotlin.Double>
public fun kotlin.FloatArray.distinct(): kotlin.collections.List<kotlin.Float>
public fun kotlin.IntArray.distinct(): kotlin.collections.List<kotlin.Int>
public fun kotlin.LongArray.distinct(): kotlin.collections.List<kotlin.Long>
public fun kotlin.ShortArray.distinct(): kotlin.collections.List<kotlin.Short>
public fun </*0*/ T> kotlin.collections.Iterable<T>.distinct(): kotlin.collections.List<T>
public inline fun </*0*/ T, /*1*/ K> kotlin.Array<out T>.distinctBy(/*0*/ selector: (T) -> K): kotlin.collections.List<T>
public inline fun </*0*/ K> kotlin.BooleanArray.distinctBy(/*0*/ selector: (kotlin.Boolean) -> K): kotlin.collections.List<kotlin.Boolean>
public inline fun </*0*/ K> kotlin.ByteArray.distinctBy(/*0*/ selector: (kotlin.Byte) -> K): kotlin.collections.List<kotlin.Byte>
public inline fun </*0*/ K> kotlin.CharArray.distinctBy(/*0*/ selector: (kotlin.Char) -> K): kotlin.collections.List<kotlin.Char>
public inline fun </*0*/ K> kotlin.DoubleArray.distinctBy(/*0*/ selector: (kotlin.Double) -> K): kotlin.collections.List<kotlin.Double>
public inline fun </*0*/ K> kotlin.FloatArray.distinctBy(/*0*/ selector: (kotlin.Float) -> K): kotlin.collections.List<kotlin.Float>
public inline fun </*0*/ K> kotlin.IntArray.distinctBy(/*0*/ selector: (kotlin.Int) -> K): kotlin.collections.List<kotlin.Int>
public inline fun </*0*/ K> kotlin.LongArray.distinctBy(/*0*/ selector: (kotlin.Long) -> K): kotlin.collections.List<kotlin.Long>
public inline fun </*0*/ K> kotlin.ShortArray.distinctBy(/*0*/ selector: (kotlin.Short) -> K): kotlin.collections.List<kotlin.Short>
public inline fun </*0*/ T, /*1*/ K> kotlin.collections.Iterable<T>.distinctBy(/*0*/ selector: (T) -> K): kotlin.collections.List<T>
public fun </*0*/ T> kotlin.Array<out T>.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<T>
public fun kotlin.BooleanArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>
public fun kotlin.ByteArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Byte>
public fun kotlin.CharArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Char>
public fun kotlin.DoubleArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Double>
public fun kotlin.FloatArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Float>
public fun kotlin.IntArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Int>
public fun kotlin.LongArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Long>
public fun kotlin.ShortArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UShort>
public fun </*0*/ T> kotlin.collections.Iterable<T>.drop(/*0*/ n: kotlin.Int): kotlin.collections.List<T>
public fun </*0*/ T> kotlin.Array<out T>.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<T>
public fun kotlin.BooleanArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>
public fun kotlin.ByteArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Byte>
public fun kotlin.CharArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Char>
public fun kotlin.DoubleArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Double>
public fun kotlin.FloatArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Float>
public fun kotlin.IntArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Int>
public fun kotlin.LongArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Long>
public fun kotlin.ShortArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<kotlin.UShort>
public fun </*0*/ T> kotlin.collections.List<T>.dropLast(/*0*/ n: kotlin.Int): kotlin.collections.List<T>
public inline fun </*0*/ T> kotlin.Array<out T>.dropLastWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun kotlin.BooleanArray.dropLastWhile(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>
public inline fun kotlin.ByteArray.dropLastWhile(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>
public inline fun kotlin.CharArray.dropLastWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>
public inline fun kotlin.DoubleArray.dropLastWhile(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>
public inline fun kotlin.FloatArray.dropLastWhile(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>
public inline fun kotlin.IntArray.dropLastWhile(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>
public inline fun kotlin.LongArray.dropLastWhile(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>
public inline fun kotlin.ShortArray.dropLastWhile(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.dropLastWhile(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.dropLastWhile(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.dropLastWhile(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.dropLastWhile(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>
public inline fun </*0*/ T> kotlin.collections.List<T>.dropLastWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun </*0*/ T> kotlin.Array<out T>.dropWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun kotlin.BooleanArray.dropWhile(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>
public inline fun kotlin.ByteArray.dropWhile(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>
public inline fun kotlin.CharArray.dropWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>
public inline fun kotlin.DoubleArray.dropWhile(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>
public inline fun kotlin.FloatArray.dropWhile(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>
public inline fun kotlin.IntArray.dropWhile(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>
public inline fun kotlin.LongArray.dropWhile(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>
public inline fun kotlin.ShortArray.dropWhile(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.dropWhile(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.dropWhile(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.dropWhile(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.dropWhile(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.dropWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T, /*1*/ K> kotlin.collections.Grouping<T, K>.eachCount(): kotlin.collections.Map<K, kotlin.Int>
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.Int>> kotlin.collections.Grouping<T, K>.eachCountTo(/*0*/ destination: M): M
public fun </*0*/ T> kotlin.Array<out T>.elementAt(/*0*/ index: kotlin.Int): T
public fun kotlin.BooleanArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Boolean
public fun kotlin.ByteArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Byte
public fun kotlin.CharArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Char
public fun kotlin.DoubleArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Double
public fun kotlin.FloatArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Float
public fun kotlin.IntArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Int
public fun kotlin.LongArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Long
public fun kotlin.ShortArray.elementAt(/*0*/ index: kotlin.Int): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.elementAt(/*0*/ index: kotlin.Int): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.elementAt(/*0*/ index: kotlin.Int): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.elementAt(/*0*/ index: kotlin.Int): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.elementAt(/*0*/ index: kotlin.Int): kotlin.UShort
public fun </*0*/ T> kotlin.collections.Iterable<T>.elementAt(/*0*/ index: kotlin.Int): T
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>.elementAt(/*0*/ index: kotlin.Int): T
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Byte): kotlin.Byte
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Double): kotlin.Double
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Float): kotlin.Float
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Int): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Long): kotlin.Long
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UShort): kotlin.UShort
public fun </*0*/ T> kotlin.collections.Iterable<T>.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.elementAtOrNull(/*0*/ index: kotlin.Int): T?
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Boolean?
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Byte?
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Char?
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Double?
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Float?
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Int?
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Long?
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.Iterable<T>.elementAtOrNull(/*0*/ index: kotlin.Int): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>.elementAtOrNull(/*0*/ index: kotlin.Int): T?
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> kotlin.Array<T>.fill(/*0*/ element: T, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.BooleanArray.fill(/*0*/ element: kotlin.Boolean, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ByteArray.fill(/*0*/ element: kotlin.Byte, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.CharArray.fill(/*0*/ element: kotlin.Char, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.DoubleArray.fill(/*0*/ element: kotlin.Double, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.FloatArray.fill(/*0*/ element: kotlin.Float, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.IntArray.fill(/*0*/ element: kotlin.Int, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.LongArray.fill(/*0*/ element: kotlin.Long, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ShortArray.fill(/*0*/ element: kotlin.Short, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.fill(/*0*/ element: kotlin.UByte, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.fill(/*0*/ element: kotlin.UInt, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.fill(/*0*/ element: kotlin.ULong, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.fill(/*0*/ element: kotlin.UShort, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.Unit
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T> kotlin.collections.MutableList<T>.fill(/*0*/ value: T): kotlin.Unit
public inline fun </*0*/ T> kotlin.Array<out T>.filter(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun kotlin.BooleanArray.filter(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>
public inline fun kotlin.ByteArray.filter(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>
public inline fun kotlin.CharArray.filter(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>
public inline fun kotlin.DoubleArray.filter(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>
public inline fun kotlin.FloatArray.filter(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>
public inline fun kotlin.IntArray.filter(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>
public inline fun kotlin.LongArray.filter(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>
public inline fun kotlin.ShortArray.filter(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.filter(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.filter(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.filter(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.filter(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.filter(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.filter(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.collections.Map<K, V>
public inline fun </*0*/ T> kotlin.Array<out T>.filterIndexed(/*0*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun kotlin.BooleanArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>
public inline fun kotlin.ByteArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>
public inline fun kotlin.CharArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>
public inline fun kotlin.DoubleArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>
public inline fun kotlin.FloatArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>
public inline fun kotlin.IntArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>
public inline fun kotlin.LongArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>
public inline fun kotlin.ShortArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.filterIndexed(/*0*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Byte) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Double) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Float) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Int) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Long) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Short) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.UByte) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.UInt) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.ULong) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.UShort) -> kotlin.Boolean): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C
public inline fun </*0*/ reified R> kotlin.Array<*>.filterIsInstance(): kotlin.collections.List<R>
public inline fun </*0*/ reified R> kotlin.collections.Iterable<*>.filterIsInstance(): kotlin.collections.List<R>
public inline fun </*0*/ reified R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<*>.filterIsInstanceTo(/*0*/ destination: C): C
public inline fun </*0*/ reified R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<*>.filterIsInstanceTo(/*0*/ destination: C): C
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.filterKeys(/*0*/ predicate: (K) -> kotlin.Boolean): kotlin.collections.Map<K, V>
public inline fun </*0*/ T> kotlin.Array<out T>.filterNot(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun kotlin.BooleanArray.filterNot(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>
public inline fun kotlin.ByteArray.filterNot(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>
public inline fun kotlin.CharArray.filterNot(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>
public inline fun kotlin.DoubleArray.filterNot(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>
public inline fun kotlin.FloatArray.filterNot(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>
public inline fun kotlin.IntArray.filterNot(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>
public inline fun kotlin.LongArray.filterNot(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>
public inline fun kotlin.ShortArray.filterNot(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.filterNot(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.filterNot(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.filterNot(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.filterNot(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.filterNot(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.filterNot(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.collections.Map<K, V>
public fun </*0*/ T : kotlin.Any> kotlin.Array<out T?>.filterNotNull(): kotlin.collections.List<T>
public fun </*0*/ T : kotlin.Any> kotlin.collections.Iterable<T?>.filterNotNull(): kotlin.collections.List<T>
public fun </*0*/ C : kotlin.collections.MutableCollection<in T>, /*1*/ T : kotlin.Any> kotlin.Array<out T?>.filterNotNullTo(/*0*/ destination: C): C
public fun </*0*/ C : kotlin.collections.MutableCollection<in T>, /*1*/ T : kotlin.Any> kotlin.collections.Iterable<T?>.filterNotNullTo(/*0*/ destination: C): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Byte) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Char) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Double) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Float) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Int) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Long) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Short) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UByte) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UInt) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.ULong) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UShort) -> kotlin.Boolean): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Map<out K, V>.filterNotTo(/*0*/ destination: M, /*1*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): M
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Byte) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Char) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Double) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Float) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Int) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Long) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Short) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UByte) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UInt) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.ULong) -> kotlin.Boolean): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.UShort) -> kotlin.Boolean): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Map<out K, V>.filterTo(/*0*/ destination: M, /*1*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): M
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.filterValues(/*0*/ predicate: (V) -> kotlin.Boolean): kotlin.collections.Map<K, V>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.find(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.find(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.find(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.find(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.find(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.find(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.find(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.find(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.find(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.find(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.find(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.find(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.find(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.find(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.findLast(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.findLast(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.findLast(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.findLast(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.findLast(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.findLast(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.findLast(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.findLast(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.findLast(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.findLast(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.findLast(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.findLast(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.findLast(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.findLast(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>.findLast(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T> kotlin.Array<out T>.first(): T
public inline fun </*0*/ T> kotlin.Array<out T>.first(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun kotlin.BooleanArray.first(): kotlin.Boolean
public inline fun kotlin.BooleanArray.first(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ByteArray.first(): kotlin.Byte
public inline fun kotlin.ByteArray.first(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte
public fun kotlin.CharArray.first(): kotlin.Char
public inline fun kotlin.CharArray.first(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char
public fun kotlin.DoubleArray.first(): kotlin.Double
public inline fun kotlin.DoubleArray.first(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double
public fun kotlin.FloatArray.first(): kotlin.Float
public inline fun kotlin.FloatArray.first(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float
public fun kotlin.IntArray.first(): kotlin.Int
public inline fun kotlin.IntArray.first(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int
public fun kotlin.LongArray.first(): kotlin.Long
public inline fun kotlin.LongArray.first(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long
public fun kotlin.ShortArray.first(): kotlin.Short
public inline fun kotlin.ShortArray.first(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.first(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.first(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.first(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.first(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.first(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.first(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.first(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.first(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort
public fun </*0*/ T> kotlin.collections.Iterable<T>.first(): T
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.first(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ T> kotlin.collections.List<T>.first(): T
public fun </*0*/ T> kotlin.Array<out T>.firstOrNull(): T?
public inline fun </*0*/ T> kotlin.Array<out T>.firstOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun kotlin.BooleanArray.firstOrNull(): kotlin.Boolean?
public inline fun kotlin.BooleanArray.firstOrNull(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
public fun kotlin.ByteArray.firstOrNull(): kotlin.Byte?
public inline fun kotlin.ByteArray.firstOrNull(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?
public fun kotlin.CharArray.firstOrNull(): kotlin.Char?
public inline fun kotlin.CharArray.firstOrNull(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.DoubleArray.firstOrNull(): kotlin.Double?
public inline fun kotlin.DoubleArray.firstOrNull(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?
public fun kotlin.FloatArray.firstOrNull(): kotlin.Float?
public inline fun kotlin.FloatArray.firstOrNull(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?
public fun kotlin.IntArray.firstOrNull(): kotlin.Int?
public inline fun kotlin.IntArray.firstOrNull(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?
public fun kotlin.LongArray.firstOrNull(): kotlin.Long?
public inline fun kotlin.LongArray.firstOrNull(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?
public fun kotlin.ShortArray.firstOrNull(): kotlin.Short?
public inline fun kotlin.ShortArray.firstOrNull(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.firstOrNull(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.firstOrNull(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.firstOrNull(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.firstOrNull(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.firstOrNull(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.firstOrNull(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.firstOrNull(): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.firstOrNull(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.Iterable<T>.firstOrNull(): T?
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.firstOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T> kotlin.collections.List<T>.firstOrNull(): T?
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.flatMap(/*0*/ transform: (T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequence") public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.flatMap(/*0*/ transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.BooleanArray.flatMap(/*0*/ transform: (kotlin.Boolean) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ByteArray.flatMap(/*0*/ transform: (kotlin.Byte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.CharArray.flatMap(/*0*/ transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.DoubleArray.flatMap(/*0*/ transform: (kotlin.Double) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.FloatArray.flatMap(/*0*/ transform: (kotlin.Float) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.IntArray.flatMap(/*0*/ transform: (kotlin.Int) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.LongArray.flatMap(/*0*/ transform: (kotlin.Long) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ShortArray.flatMap(/*0*/ transform: (kotlin.Short) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.flatMap(/*0*/ transform: (kotlin.UByte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.flatMap(/*0*/ transform: (kotlin.UInt) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.flatMap(/*0*/ transform: (kotlin.ULong) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.flatMap(/*0*/ transform: (kotlin.UShort) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.flatMap(/*0*/ transform: (T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequence") public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.flatMap(/*0*/ transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>
public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.flatMap(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequence") public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.flatMap(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequenceTo") public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.sequences.Sequence<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Boolean) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Byte) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Double) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Float) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Int) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Long) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Short) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UByte) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UInt) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.ULong) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UShort) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequenceTo") public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.sequences.Sequence<R>): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ R, /*3*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.collections.Iterable<R>): C
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapSequenceTo") public inline fun </*0*/ K, /*1*/ V, /*2*/ R, /*3*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.sequences.Sequence<R>): C
public fun </*0*/ T> kotlin.Array<out kotlin.Array<out T>>.flatten(): kotlin.collections.List<T>
public fun </*0*/ T> kotlin.collections.Iterable<kotlin.collections.Iterable<T>>.flatten(): kotlin.collections.List<T>
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, T) -> R): R
public inline fun </*0*/ R> kotlin.BooleanArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Boolean) -> R): R
public inline fun </*0*/ R> kotlin.ByteArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Byte) -> R): R
public inline fun </*0*/ R> kotlin.CharArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Char) -> R): R
public inline fun </*0*/ R> kotlin.DoubleArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Double) -> R): R
public inline fun </*0*/ R> kotlin.FloatArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Float) -> R): R
public inline fun </*0*/ R> kotlin.IntArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Int) -> R): R
public inline fun </*0*/ R> kotlin.LongArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Long) -> R): R
public inline fun </*0*/ R> kotlin.ShortArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.UShort) -> R): R
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R> kotlin.collections.Grouping<T, K>.fold(/*0*/ initialValueSelector: (key: K, element: T) -> R, /*1*/ operation: (key: K, accumulator: R, element: T) -> R): kotlin.collections.Map<K, R>
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R> kotlin.collections.Grouping<T, K>.fold(/*0*/ initialValue: R, /*1*/ operation: (accumulator: R, element: T) -> R): kotlin.collections.Map<K, R>
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, T) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, T) -> R): R
public inline fun </*0*/ R> kotlin.BooleanArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Boolean) -> R): R
public inline fun </*0*/ R> kotlin.ByteArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Byte) -> R): R
public inline fun </*0*/ R> kotlin.CharArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): R
public inline fun </*0*/ R> kotlin.DoubleArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Double) -> R): R
public inline fun </*0*/ R> kotlin.FloatArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Float) -> R): R
public inline fun </*0*/ R> kotlin.IntArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Int) -> R): R
public inline fun </*0*/ R> kotlin.LongArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Long) -> R): R
public inline fun </*0*/ R> kotlin.ShortArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.UShort) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, T) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.foldRight(/*0*/ initial: R, /*1*/ operation: (T, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.BooleanArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Boolean, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.ByteArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Byte, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.CharArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Char, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.DoubleArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Double, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.FloatArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Float, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.IntArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Int, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.LongArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Long, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.ShortArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Short, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.UByte, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.UInt, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.ULong, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.UShort, acc: R) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.List<T>.foldRight(/*0*/ initial: R, /*1*/ operation: (T, acc: R) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, T, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.BooleanArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Boolean, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.ByteArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Byte, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.CharArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Char, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.DoubleArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Double, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.FloatArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Float, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.IntArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Int, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.LongArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Long, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.ShortArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Short, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.UByte, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.UInt, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.ULong, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.UShort, acc: R) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.List<T>.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, T, acc: R) -> R): R
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R, /*3*/ M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.foldTo(/*0*/ destination: M, /*1*/ initialValueSelector: (key: K, element: T) -> R, /*2*/ operation: (key: K, accumulator: R, element: T) -> R): M
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K, /*2*/ R, /*3*/ M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.foldTo(/*0*/ destination: M, /*1*/ initialValue: R, /*2*/ operation: (accumulator: R, element: T) -> R): M
public inline fun </*0*/ T> kotlin.Array<out T>.forEach(/*0*/ action: (T) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.BooleanArray.forEach(/*0*/ action: (kotlin.Boolean) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.ByteArray.forEach(/*0*/ action: (kotlin.Byte) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.CharArray.forEach(/*0*/ action: (kotlin.Char) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.DoubleArray.forEach(/*0*/ action: (kotlin.Double) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.FloatArray.forEach(/*0*/ action: (kotlin.Float) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.IntArray.forEach(/*0*/ action: (kotlin.Int) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.LongArray.forEach(/*0*/ action: (kotlin.Long) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.ShortArray.forEach(/*0*/ action: (kotlin.Short) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.forEach(/*0*/ action: (kotlin.UByte) -> kotlin.Unit): kotlin.Unit
