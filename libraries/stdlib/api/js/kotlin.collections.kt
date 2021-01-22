public val <T> kotlin.Array<out T>.indices: kotlin.ranges.IntRange { get; }

public val kotlin.BooleanArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.ByteArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.CharArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.DoubleArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.FloatArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.IntArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.LongArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.ShortArray.indices: kotlin.ranges.IntRange { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UByteArray.indices: kotlin.ranges.IntRange { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UIntArray.indices: kotlin.ranges.IntRange { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.ULongArray.indices: kotlin.ranges.IntRange { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UShortArray.indices: kotlin.ranges.IntRange { get; }

public val kotlin.collections.Collection<*>.indices: kotlin.ranges.IntRange { get; }

public val <T> kotlin.Array<out T>.lastIndex: kotlin.Int { get; }

public val kotlin.BooleanArray.lastIndex: kotlin.Int { get; }

public val kotlin.ByteArray.lastIndex: kotlin.Int { get; }

public val kotlin.CharArray.lastIndex: kotlin.Int { get; }

public val kotlin.DoubleArray.lastIndex: kotlin.Int { get; }

public val kotlin.FloatArray.lastIndex: kotlin.Int { get; }

public val kotlin.IntArray.lastIndex: kotlin.Int { get; }

public val kotlin.LongArray.lastIndex: kotlin.Int { get; }

public val kotlin.ShortArray.lastIndex: kotlin.Int { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UByteArray.lastIndex: kotlin.Int { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UIntArray.lastIndex: kotlin.Int { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.ULongArray.lastIndex: kotlin.Int { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public val kotlin.UShortArray.lastIndex: kotlin.Int { get; }

public val <T> kotlin.collections.List<T>.lastIndex: kotlin.Int { get; }

@kotlin.internal.InlineOnly
public inline fun <T> Iterable(crossinline iterator: () -> kotlin.collections.Iterator<T>): kotlin.collections.Iterable<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> List(size: kotlin.Int, init: (index: kotlin.Int) -> T): kotlin.collections.List<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> MutableList(size: kotlin.Int, init: (index: kotlin.Int) -> T): kotlin.collections.MutableList<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> arrayListOf(): kotlin.collections.ArrayList<T>

public fun <T> arrayListOf(vararg elements: T): kotlin.collections.ArrayList<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildList(capacity: kotlin.Int, @kotlin.BuilderInference
builderAction: kotlin.collections.MutableList<E>.() -> kotlin.Unit): kotlin.collections.List<E>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildList(@kotlin.BuilderInference
builderAction: kotlin.collections.MutableList<E>.() -> kotlin.Unit): kotlin.collections.List<E>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <K, V> buildMap(capacity: kotlin.Int, @kotlin.BuilderInference
builderAction: kotlin.collections.MutableMap<K, V>.() -> kotlin.Unit): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <K, V> buildMap(@kotlin.BuilderInference
builderAction: kotlin.collections.MutableMap<K, V>.() -> kotlin.Unit): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildSet(capacity: kotlin.Int, @kotlin.BuilderInference
builderAction: kotlin.collections.MutableSet<E>.() -> kotlin.Unit): kotlin.collections.Set<E>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildSet(@kotlin.BuilderInference
builderAction: kotlin.collections.MutableSet<E>.() -> kotlin.Unit): kotlin.collections.Set<E>

public fun <T> emptyList(): kotlin.collections.List<T>

public fun <K, V> emptyMap(): kotlin.collections.Map<K, V>

public fun <T> emptySet(): kotlin.collections.Set<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> hashMapOf(): kotlin.collections.HashMap<K, V>

public fun <K, V> hashMapOf(vararg pairs: kotlin.Pair<K, V>): kotlin.collections.HashMap<K, V>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> hashSetOf(): kotlin.collections.HashSet<T>

public fun <T> hashSetOf(vararg elements: T): kotlin.collections.HashSet<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> linkedMapOf(): kotlin.collections.LinkedHashMap<K, V>

public fun <K, V> linkedMapOf(vararg pairs: kotlin.Pair<K, V>): kotlin.collections.LinkedHashMap<K, V>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> linkedSetOf(): kotlin.collections.LinkedHashSet<T>

public fun <T> linkedSetOf(vararg elements: T): kotlin.collections.LinkedHashSet<T>

public fun <V> linkedStringMapOf(vararg pairs: kotlin.Pair<kotlin.String, V>): kotlin.collections.LinkedHashMap<kotlin.String, V>

public fun linkedStringSetOf(vararg elements: kotlin.String): kotlin.collections.LinkedHashSet<kotlin.String>

@kotlin.internal.InlineOnly
public inline fun <T> listOf(): kotlin.collections.List<T>

public fun <T> listOf(element: T): kotlin.collections.List<T>

public fun <T> listOf(vararg elements: T): kotlin.collections.List<T>

public fun <T : kotlin.Any> listOfNotNull(element: T?): kotlin.collections.List<T>

public fun <T : kotlin.Any> listOfNotNull(vararg elements: T?): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun <K, V> mapOf(): kotlin.collections.Map<K, V>

public fun <K, V> mapOf(vararg pairs: kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public fun <K, V> mapOf(pair: kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> mutableListOf(): kotlin.collections.MutableList<T>

public fun <T> mutableListOf(vararg elements: T): kotlin.collections.MutableList<T>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> mutableMapOf(): kotlin.collections.MutableMap<K, V>

public fun <K, V> mutableMapOf(vararg pairs: kotlin.Pair<K, V>): kotlin.collections.MutableMap<K, V>

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun <T> mutableSetOf(): kotlin.collections.MutableSet<T>

public fun <T> mutableSetOf(vararg elements: T): kotlin.collections.MutableSet<T>

@kotlin.internal.InlineOnly
public inline fun <T> setOf(): kotlin.collections.Set<T>

public fun <T> setOf(element: T): kotlin.collections.Set<T>

public fun <T> setOf(vararg elements: T): kotlin.collections.Set<T>

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Any> setOfNotNull(element: T?): kotlin.collections.Set<T>

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Any> setOfNotNull(vararg elements: T?): kotlin.collections.Set<T>

public fun <V> stringMapOf(vararg pairs: kotlin.Pair<kotlin.String, V>): kotlin.collections.HashMap<kotlin.String, V>

public fun stringSetOf(vararg elements: kotlin.String): kotlin.collections.HashSet<kotlin.String>

public fun <T> kotlin.collections.MutableCollection<in T>.addAll(elements: kotlin.Array<out T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.addAll(elements: kotlin.collections.Iterable<T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.addAll(elements: kotlin.sequences.Sequence<T>): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R> kotlin.collections.Grouping<T, K>.aggregate(operation: (key: K, accumulator: R?, element: T, first: kotlin.Boolean) -> R): kotlin.collections.Map<K, R>

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R, M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.aggregateTo(destination: M, operation: (key: K, accumulator: R?, element: T, first: kotlin.Boolean) -> R): M

public inline fun <T> kotlin.Array<out T>.all(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.BooleanArray.all(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ByteArray.all(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.CharArray.all(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.DoubleArray.all(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.FloatArray.all(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.IntArray.all(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.LongArray.all(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ShortArray.all(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.all(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.all(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.all(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.all(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean

public inline fun <T> kotlin.collections.Iterable<T>.all(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public inline fun <K, V> kotlin.collections.Map<out K, V>.all(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.Array<out T>.any(): kotlin.Boolean

public inline fun <T> kotlin.Array<out T>.any(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.BooleanArray.any(): kotlin.Boolean

public inline fun kotlin.BooleanArray.any(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ByteArray.any(): kotlin.Boolean

public inline fun kotlin.ByteArray.any(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.CharArray.any(): kotlin.Boolean

public inline fun kotlin.CharArray.any(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.DoubleArray.any(): kotlin.Boolean

public inline fun kotlin.DoubleArray.any(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.FloatArray.any(): kotlin.Boolean

public inline fun kotlin.FloatArray.any(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.IntArray.any(): kotlin.Boolean

public inline fun kotlin.IntArray.any(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.LongArray.any(): kotlin.Boolean

public inline fun kotlin.LongArray.any(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ShortArray.any(): kotlin.Boolean

public inline fun kotlin.ShortArray.any(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.any(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.any(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.any(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.any(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.any(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.any(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.any(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.any(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.collections.Iterable<T>.any(): kotlin.Boolean

public inline fun <T> kotlin.collections.Iterable<T>.any(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <K, V> kotlin.collections.Map<out K, V>.any(): kotlin.Boolean

public inline fun <K, V> kotlin.collections.Map<out K, V>.any(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.asByteArray(): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.asIntArray(): kotlin.IntArray

public fun <T> kotlin.Array<out T>.asIterable(): kotlin.collections.Iterable<T>

public fun kotlin.BooleanArray.asIterable(): kotlin.collections.Iterable<kotlin.Boolean>

public fun kotlin.ByteArray.asIterable(): kotlin.collections.Iterable<kotlin.Byte>

public fun kotlin.CharArray.asIterable(): kotlin.collections.Iterable<kotlin.Char>

public fun kotlin.DoubleArray.asIterable(): kotlin.collections.Iterable<kotlin.Double>

public fun kotlin.FloatArray.asIterable(): kotlin.collections.Iterable<kotlin.Float>

public fun kotlin.IntArray.asIterable(): kotlin.collections.Iterable<kotlin.Int>

public fun kotlin.LongArray.asIterable(): kotlin.collections.Iterable<kotlin.Long>

public fun kotlin.ShortArray.asIterable(): kotlin.collections.Iterable<kotlin.Short>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.asIterable(): kotlin.collections.Iterable<T>

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.asIterable(): kotlin.collections.Iterable<kotlin.collections.Map.Entry<K, V>>

public fun <T> kotlin.Array<out T>.asList(): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.asList(): kotlin.collections.List<kotlin.Boolean>

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.asList(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.asList(): kotlin.collections.List<kotlin.Char>

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.asList(): kotlin.collections.List<kotlin.Double>

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.asList(): kotlin.collections.List<kotlin.Float>

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.asList(): kotlin.collections.List<kotlin.Int>

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.asList(): kotlin.collections.List<kotlin.Long>

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.asList(): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.asList(): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.asList(): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.asList(): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.asList(): kotlin.collections.List<kotlin.UShort>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.asLongArray(): kotlin.LongArray

public fun <T> kotlin.collections.List<T>.asReversed(): kotlin.collections.List<T>

@kotlin.jvm.JvmName(name = "asReversedMutable")
public fun <T> kotlin.collections.MutableList<T>.asReversed(): kotlin.collections.MutableList<T>

public fun <T> kotlin.Array<out T>.asSequence(): kotlin.sequences.Sequence<T>

public fun kotlin.BooleanArray.asSequence(): kotlin.sequences.Sequence<kotlin.Boolean>

public fun kotlin.ByteArray.asSequence(): kotlin.sequences.Sequence<kotlin.Byte>

public fun kotlin.CharArray.asSequence(): kotlin.sequences.Sequence<kotlin.Char>

public fun kotlin.DoubleArray.asSequence(): kotlin.sequences.Sequence<kotlin.Double>

public fun kotlin.FloatArray.asSequence(): kotlin.sequences.Sequence<kotlin.Float>

public fun kotlin.IntArray.asSequence(): kotlin.sequences.Sequence<kotlin.Int>

public fun kotlin.LongArray.asSequence(): kotlin.sequences.Sequence<kotlin.Long>

public fun kotlin.ShortArray.asSequence(): kotlin.sequences.Sequence<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.asSequence(): kotlin.sequences.Sequence<T>

public fun <K, V> kotlin.collections.Map<out K, V>.asSequence(): kotlin.sequences.Sequence<kotlin.collections.Map.Entry<K, V>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.asShortArray(): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.asUByteArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.asUIntArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.asULongArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.asUShortArray(): kotlin.UShortArray

public inline fun <T, K, V> kotlin.Array<out T>.associate(transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.BooleanArray.associate(transform: (kotlin.Boolean) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.ByteArray.associate(transform: (kotlin.Byte) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.CharArray.associate(transform: (kotlin.Char) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.DoubleArray.associate(transform: (kotlin.Double) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.FloatArray.associate(transform: (kotlin.Float) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.IntArray.associate(transform: (kotlin.Int) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.LongArray.associate(transform: (kotlin.Long) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K, V> kotlin.ShortArray.associate(transform: (kotlin.Short) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <T, K, V> kotlin.collections.Iterable<T>.associate(transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <T, K> kotlin.Array<out T>.associateBy(keySelector: (T) -> K): kotlin.collections.Map<K, T>

public inline fun <T, K, V> kotlin.Array<out T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.BooleanArray.associateBy(keySelector: (kotlin.Boolean) -> K): kotlin.collections.Map<K, kotlin.Boolean>

public inline fun <K, V> kotlin.BooleanArray.associateBy(keySelector: (kotlin.Boolean) -> K, valueTransform: (kotlin.Boolean) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.ByteArray.associateBy(keySelector: (kotlin.Byte) -> K): kotlin.collections.Map<K, kotlin.Byte>

public inline fun <K, V> kotlin.ByteArray.associateBy(keySelector: (kotlin.Byte) -> K, valueTransform: (kotlin.Byte) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.CharArray.associateBy(keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.Char>

public inline fun <K, V> kotlin.CharArray.associateBy(keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.DoubleArray.associateBy(keySelector: (kotlin.Double) -> K): kotlin.collections.Map<K, kotlin.Double>

public inline fun <K, V> kotlin.DoubleArray.associateBy(keySelector: (kotlin.Double) -> K, valueTransform: (kotlin.Double) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.FloatArray.associateBy(keySelector: (kotlin.Float) -> K): kotlin.collections.Map<K, kotlin.Float>

public inline fun <K, V> kotlin.FloatArray.associateBy(keySelector: (kotlin.Float) -> K, valueTransform: (kotlin.Float) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.IntArray.associateBy(keySelector: (kotlin.Int) -> K): kotlin.collections.Map<K, kotlin.Int>

public inline fun <K, V> kotlin.IntArray.associateBy(keySelector: (kotlin.Int) -> K, valueTransform: (kotlin.Int) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.LongArray.associateBy(keySelector: (kotlin.Long) -> K): kotlin.collections.Map<K, kotlin.Long>

public inline fun <K, V> kotlin.LongArray.associateBy(keySelector: (kotlin.Long) -> K, valueTransform: (kotlin.Long) -> V): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.ShortArray.associateBy(keySelector: (kotlin.Short) -> K): kotlin.collections.Map<K, kotlin.Short>

public inline fun <K, V> kotlin.ShortArray.associateBy(keySelector: (kotlin.Short) -> K, valueTransform: (kotlin.Short) -> V): kotlin.collections.Map<K, V>

public inline fun <T, K> kotlin.collections.Iterable<T>.associateBy(keySelector: (T) -> K): kotlin.collections.Map<K, T>

public inline fun <T, K, V> kotlin.collections.Iterable<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, V>

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, in T>> kotlin.Array<out T>.associateByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Boolean>> kotlin.BooleanArray.associateByTo(destination: M, keySelector: (kotlin.Boolean) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.BooleanArray.associateByTo(destination: M, keySelector: (kotlin.Boolean) -> K, valueTransform: (kotlin.Boolean) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Byte>> kotlin.ByteArray.associateByTo(destination: M, keySelector: (kotlin.Byte) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.ByteArray.associateByTo(destination: M, keySelector: (kotlin.Byte) -> K, valueTransform: (kotlin.Byte) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Char>> kotlin.CharArray.associateByTo(destination: M, keySelector: (kotlin.Char) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharArray.associateByTo(destination: M, keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Double>> kotlin.DoubleArray.associateByTo(destination: M, keySelector: (kotlin.Double) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.DoubleArray.associateByTo(destination: M, keySelector: (kotlin.Double) -> K, valueTransform: (kotlin.Double) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Float>> kotlin.FloatArray.associateByTo(destination: M, keySelector: (kotlin.Float) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.FloatArray.associateByTo(destination: M, keySelector: (kotlin.Float) -> K, valueTransform: (kotlin.Float) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Int>> kotlin.IntArray.associateByTo(destination: M, keySelector: (kotlin.Int) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.IntArray.associateByTo(destination: M, keySelector: (kotlin.Int) -> K, valueTransform: (kotlin.Int) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Long>> kotlin.LongArray.associateByTo(destination: M, keySelector: (kotlin.Long) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.LongArray.associateByTo(destination: M, keySelector: (kotlin.Long) -> K, valueTransform: (kotlin.Long) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Short>> kotlin.ShortArray.associateByTo(destination: M, keySelector: (kotlin.Short) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.ShortArray.associateByTo(destination: M, keySelector: (kotlin.Short) -> K, valueTransform: (kotlin.Short) -> V): M

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, in T>> kotlin.collections.Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out T>.associateTo(destination: M, transform: (T) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.BooleanArray.associateTo(destination: M, transform: (kotlin.Boolean) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.ByteArray.associateTo(destination: M, transform: (kotlin.Byte) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharArray.associateTo(destination: M, transform: (kotlin.Char) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.DoubleArray.associateTo(destination: M, transform: (kotlin.Double) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.FloatArray.associateTo(destination: M, transform: (kotlin.Float) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.IntArray.associateTo(destination: M, transform: (kotlin.Int) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.LongArray.associateTo(destination: M, transform: (kotlin.Long) -> kotlin.Pair<K, V>): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.ShortArray.associateTo(destination: M, transform: (kotlin.Short) -> kotlin.Pair<K, V>): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<T>.associateTo(destination: M, transform: (T) -> kotlin.Pair<K, V>): M

@kotlin.SinceKotlin(version = "1.4")
public inline fun <K, V> kotlin.Array<out K>.associateWith(valueSelector: (K) -> V): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.BooleanArray.associateWith(valueSelector: (kotlin.Boolean) -> V): kotlin.collections.Map<kotlin.Boolean, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.ByteArray.associateWith(valueSelector: (kotlin.Byte) -> V): kotlin.collections.Map<kotlin.Byte, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.CharArray.associateWith(valueSelector: (kotlin.Char) -> V): kotlin.collections.Map<kotlin.Char, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.DoubleArray.associateWith(valueSelector: (kotlin.Double) -> V): kotlin.collections.Map<kotlin.Double, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.FloatArray.associateWith(valueSelector: (kotlin.Float) -> V): kotlin.collections.Map<kotlin.Float, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.IntArray.associateWith(valueSelector: (kotlin.Int) -> V): kotlin.collections.Map<kotlin.Int, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.LongArray.associateWith(valueSelector: (kotlin.Long) -> V): kotlin.collections.Map<kotlin.Long, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.ShortArray.associateWith(valueSelector: (kotlin.Short) -> V): kotlin.collections.Map<kotlin.Short, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UByteArray.associateWith(valueSelector: (kotlin.UByte) -> V): kotlin.collections.Map<kotlin.UByte, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UIntArray.associateWith(valueSelector: (kotlin.UInt) -> V): kotlin.collections.Map<kotlin.UInt, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.ULongArray.associateWith(valueSelector: (kotlin.ULong) -> V): kotlin.collections.Map<kotlin.ULong, V>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UShortArray.associateWith(valueSelector: (kotlin.UShort) -> V): kotlin.collections.Map<kotlin.UShort, V>

@kotlin.SinceKotlin(version = "1.3")
public inline fun <K, V> kotlin.collections.Iterable<K>.associateWith(valueSelector: (K) -> V): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out K>.associateWithTo(destination: M, valueSelector: (K) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Boolean, in V>> kotlin.BooleanArray.associateWithTo(destination: M, valueSelector: (kotlin.Boolean) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Byte, in V>> kotlin.ByteArray.associateWithTo(destination: M, valueSelector: (kotlin.Byte) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Char, in V>> kotlin.CharArray.associateWithTo(destination: M, valueSelector: (kotlin.Char) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Double, in V>> kotlin.DoubleArray.associateWithTo(destination: M, valueSelector: (kotlin.Double) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Float, in V>> kotlin.FloatArray.associateWithTo(destination: M, valueSelector: (kotlin.Float) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Int, in V>> kotlin.IntArray.associateWithTo(destination: M, valueSelector: (kotlin.Int) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Long, in V>> kotlin.LongArray.associateWithTo(destination: M, valueSelector: (kotlin.Long) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Short, in V>> kotlin.ShortArray.associateWithTo(destination: M, valueSelector: (kotlin.Short) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.UByte, in V>> kotlin.UByteArray.associateWithTo(destination: M, valueSelector: (kotlin.UByte) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.UInt, in V>> kotlin.UIntArray.associateWithTo(destination: M, valueSelector: (kotlin.UInt) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.ULong, in V>> kotlin.ULongArray.associateWithTo(destination: M, valueSelector: (kotlin.ULong) -> V): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.UShort, in V>> kotlin.UShortArray.associateWithTo(destination: M, valueSelector: (kotlin.UShort) -> V): M

@kotlin.SinceKotlin(version = "1.3")
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<K>.associateWithTo(destination: M, valueSelector: (K) -> V): M

@kotlin.jvm.JvmName(name = "averageOfByte")
public fun kotlin.Array<out kotlin.Byte>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfDouble")
public fun kotlin.Array<out kotlin.Double>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfFloat")
public fun kotlin.Array<out kotlin.Float>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfInt")
public fun kotlin.Array<out kotlin.Int>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfLong")
public fun kotlin.Array<out kotlin.Long>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfShort")
public fun kotlin.Array<out kotlin.Short>.average(): kotlin.Double

public fun kotlin.ByteArray.average(): kotlin.Double

public fun kotlin.DoubleArray.average(): kotlin.Double

public fun kotlin.FloatArray.average(): kotlin.Double

public fun kotlin.IntArray.average(): kotlin.Double

public fun kotlin.LongArray.average(): kotlin.Double

public fun kotlin.ShortArray.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfByte")
public fun kotlin.collections.Iterable<kotlin.Byte>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfDouble")
public fun kotlin.collections.Iterable<kotlin.Double>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfFloat")
public fun kotlin.collections.Iterable<kotlin.Float>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfInt")
public fun kotlin.collections.Iterable<kotlin.Int>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfLong")
public fun kotlin.collections.Iterable<kotlin.Long>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfShort")
public fun kotlin.collections.Iterable<kotlin.Short>.average(): kotlin.Double

public fun <T> kotlin.collections.List<T>.binarySearch(element: T, comparator: kotlin.Comparator<in T>, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Int

public fun <T> kotlin.collections.List<T>.binarySearch(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ..., comparison: (T) -> kotlin.Int): kotlin.Int

public fun <T : kotlin.Comparable<T>> kotlin.collections.List<T?>.binarySearch(element: T?, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Int

public inline fun <T, K : kotlin.Comparable<K>> kotlin.collections.List<T>.binarySearchBy(key: K?, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ..., crossinline selector: (T) -> K?): kotlin.Int

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.Iterable<T>.chunked(size: kotlin.Int): kotlin.collections.List<kotlin.collections.List<T>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T, R> kotlin.collections.Iterable<T>.chunked(size: kotlin.Int, transform: (kotlin.collections.List<T>) -> R): kotlin.collections.List<R>

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Array<out T>.component1(): T

@kotlin.internal.InlineOnly
public inline operator fun kotlin.BooleanArray.component1(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ByteArray.component1(): kotlin.Byte

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharArray.component1(): kotlin.Char

@kotlin.internal.InlineOnly
public inline operator fun kotlin.DoubleArray.component1(): kotlin.Double

@kotlin.internal.InlineOnly
public inline operator fun kotlin.FloatArray.component1(): kotlin.Float

@kotlin.internal.InlineOnly
public inline operator fun kotlin.IntArray.component1(): kotlin.Int

@kotlin.internal.InlineOnly
public inline operator fun kotlin.LongArray.component1(): kotlin.Long

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ShortArray.component1(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.component1(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.component1(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.component1(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.component1(): kotlin.UShort

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.List<T>.component1(): T

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.Map.Entry<K, V>.component1(): K

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Array<out T>.component2(): T

@kotlin.internal.InlineOnly
public inline operator fun kotlin.BooleanArray.component2(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ByteArray.component2(): kotlin.Byte

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharArray.component2(): kotlin.Char

@kotlin.internal.InlineOnly
public inline operator fun kotlin.DoubleArray.component2(): kotlin.Double

@kotlin.internal.InlineOnly
public inline operator fun kotlin.FloatArray.component2(): kotlin.Float

@kotlin.internal.InlineOnly
public inline operator fun kotlin.IntArray.component2(): kotlin.Int

@kotlin.internal.InlineOnly
public inline operator fun kotlin.LongArray.component2(): kotlin.Long

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ShortArray.component2(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.component2(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.component2(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.component2(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.component2(): kotlin.UShort

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.List<T>.component2(): T

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.Map.Entry<K, V>.component2(): V

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Array<out T>.component3(): T

@kotlin.internal.InlineOnly
public inline operator fun kotlin.BooleanArray.component3(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ByteArray.component3(): kotlin.Byte

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharArray.component3(): kotlin.Char

@kotlin.internal.InlineOnly
public inline operator fun kotlin.DoubleArray.component3(): kotlin.Double

@kotlin.internal.InlineOnly
public inline operator fun kotlin.FloatArray.component3(): kotlin.Float

@kotlin.internal.InlineOnly
public inline operator fun kotlin.IntArray.component3(): kotlin.Int

@kotlin.internal.InlineOnly
public inline operator fun kotlin.LongArray.component3(): kotlin.Long

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ShortArray.component3(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.component3(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.component3(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.component3(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.component3(): kotlin.UShort

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.List<T>.component3(): T

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Array<out T>.component4(): T

@kotlin.internal.InlineOnly
public inline operator fun kotlin.BooleanArray.component4(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ByteArray.component4(): kotlin.Byte

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharArray.component4(): kotlin.Char

@kotlin.internal.InlineOnly
public inline operator fun kotlin.DoubleArray.component4(): kotlin.Double

@kotlin.internal.InlineOnly
public inline operator fun kotlin.FloatArray.component4(): kotlin.Float

@kotlin.internal.InlineOnly
public inline operator fun kotlin.IntArray.component4(): kotlin.Int

@kotlin.internal.InlineOnly
public inline operator fun kotlin.LongArray.component4(): kotlin.Long

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ShortArray.component4(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.component4(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.component4(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.component4(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.component4(): kotlin.UShort

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.List<T>.component4(): T

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.Array<out T>.component5(): T

@kotlin.internal.InlineOnly
public inline operator fun kotlin.BooleanArray.component5(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ByteArray.component5(): kotlin.Byte

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharArray.component5(): kotlin.Char

@kotlin.internal.InlineOnly
public inline operator fun kotlin.DoubleArray.component5(): kotlin.Double

@kotlin.internal.InlineOnly
public inline operator fun kotlin.FloatArray.component5(): kotlin.Float

@kotlin.internal.InlineOnly
public inline operator fun kotlin.IntArray.component5(): kotlin.Int

@kotlin.internal.InlineOnly
public inline operator fun kotlin.LongArray.component5(): kotlin.Long

@kotlin.internal.InlineOnly
public inline operator fun kotlin.ShortArray.component5(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.component5(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.component5(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.component5(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.component5(): kotlin.UShort

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.List<T>.component5(): T

public operator fun <@kotlin.internal.OnlyInputTypes
T> kotlin.Array<out T>.contains(element: T): kotlin.Boolean

public operator fun kotlin.BooleanArray.contains(element: kotlin.Boolean): kotlin.Boolean

public operator fun kotlin.ByteArray.contains(element: kotlin.Byte): kotlin.Boolean

public operator fun kotlin.CharArray.contains(element: kotlin.Char): kotlin.Boolean

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'any { it == element }' instead to continue using this behavior, or '.asList().contains(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "any { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public operator fun kotlin.DoubleArray.contains(element: kotlin.Double): kotlin.Boolean

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'any { it == element }' instead to continue using this behavior, or '.asList().contains(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "any { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public operator fun kotlin.FloatArray.contains(element: kotlin.Float): kotlin.Boolean

public operator fun kotlin.IntArray.contains(element: kotlin.Int): kotlin.Boolean

public operator fun kotlin.LongArray.contains(element: kotlin.Long): kotlin.Boolean

public operator fun kotlin.ShortArray.contains(element: kotlin.Short): kotlin.Boolean

public operator fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.Iterable<T>.contains(element: T): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes
K, V> kotlin.collections.Map<out K, V>.contains(key: K): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.Collection<T>.containsAll(elements: kotlin.collections.Collection<T>): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
K> kotlin.collections.Map<out K, *>.containsKey(key: K): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <K, @kotlin.internal.OnlyInputTypes
V> kotlin.collections.Map<K, V>.containsValue(value: V): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.LowPriorityInOverloadResolution
public infix fun <T> kotlin.Array<out T>.contentDeepEquals(other: kotlin.Array<out T>): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun <T> kotlin.Array<out T>?.contentDeepEquals(other: kotlin.Array<out T>?): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <T> kotlin.Array<out T>.contentDeepHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>?.contentDeepHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <T> kotlin.Array<out T>.contentDeepToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>?.contentDeepToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun <T> kotlin.Array<out T>.contentEquals(other: kotlin.Array<out T>): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun <T> kotlin.Array<out T>?.contentEquals(other: kotlin.Array<out T>?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.BooleanArray.contentEquals(other: kotlin.BooleanArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.BooleanArray?.contentEquals(other: kotlin.BooleanArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.ByteArray.contentEquals(other: kotlin.ByteArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.ByteArray?.contentEquals(other: kotlin.ByteArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.CharArray.contentEquals(other: kotlin.CharArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.CharArray?.contentEquals(other: kotlin.CharArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.DoubleArray.contentEquals(other: kotlin.DoubleArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.DoubleArray?.contentEquals(other: kotlin.DoubleArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.FloatArray.contentEquals(other: kotlin.FloatArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.FloatArray?.contentEquals(other: kotlin.FloatArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.IntArray.contentEquals(other: kotlin.IntArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.IntArray?.contentEquals(other: kotlin.IntArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.LongArray.contentEquals(other: kotlin.LongArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.LongArray?.contentEquals(other: kotlin.LongArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public infix fun kotlin.ShortArray.contentEquals(other: kotlin.ShortArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
public infix fun kotlin.ShortArray?.contentEquals(other: kotlin.ShortArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UByteArray.contentEquals(other: kotlin.UByteArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UByteArray?.contentEquals(other: kotlin.UByteArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UIntArray.contentEquals(other: kotlin.UIntArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UIntArray?.contentEquals(other: kotlin.UIntArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.ULongArray.contentEquals(other: kotlin.ULongArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.ULongArray?.contentEquals(other: kotlin.ULongArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UShortArray.contentEquals(other: kotlin.UShortArray): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UShortArray?.contentEquals(other: kotlin.UShortArray?): kotlin.Boolean

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun <T> kotlin.Array<out T>.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.BooleanArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.ByteArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.CharArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.DoubleArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.FloatArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.IntArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.LongArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.ShortArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.contentHashCode(): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray?.contentHashCode(): kotlin.Int

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun <T> kotlin.Array<out T>.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.BooleanArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.ByteArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.CharArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.DoubleArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.FloatArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.IntArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.LongArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.1")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
public fun kotlin.ShortArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray?.contentToString(): kotlin.String

@kotlin.Deprecated(message = "Use Kotlin compiler 1.4 to avoid deprecation warning.")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.DeprecatedSinceKotlin(hiddenSince = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray?.contentToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.copyInto(destination: kotlin.Array<T>, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Array<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.copyInto(destination: kotlin.BooleanArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.BooleanArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.copyInto(destination: kotlin.ByteArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.copyInto(destination: kotlin.CharArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.CharArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.copyInto(destination: kotlin.DoubleArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.DoubleArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.copyInto(destination: kotlin.FloatArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.FloatArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.copyInto(destination: kotlin.IntArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.IntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.copyInto(destination: kotlin.LongArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.LongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.copyInto(destination: kotlin.ShortArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.copyInto(destination: kotlin.UByteArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.copyInto(destination: kotlin.UIntArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.copyInto(destination: kotlin.ULongArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.copyInto(destination: kotlin.UShortArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.UShortArray

public inline fun <T> kotlin.Array<out T>.copyOf(): kotlin.Array<T>

public fun <T> kotlin.Array<out T>.copyOf(newSize: kotlin.Int): kotlin.Array<T?>

public fun kotlin.BooleanArray.copyOf(): kotlin.BooleanArray

public fun kotlin.BooleanArray.copyOf(newSize: kotlin.Int): kotlin.BooleanArray

public inline fun kotlin.ByteArray.copyOf(): kotlin.ByteArray

public fun kotlin.ByteArray.copyOf(newSize: kotlin.Int): kotlin.ByteArray

public fun kotlin.CharArray.copyOf(): kotlin.CharArray

public fun kotlin.CharArray.copyOf(newSize: kotlin.Int): kotlin.CharArray

public inline fun kotlin.DoubleArray.copyOf(): kotlin.DoubleArray

public fun kotlin.DoubleArray.copyOf(newSize: kotlin.Int): kotlin.DoubleArray

public inline fun kotlin.FloatArray.copyOf(): kotlin.FloatArray

public fun kotlin.FloatArray.copyOf(newSize: kotlin.Int): kotlin.FloatArray

public inline fun kotlin.IntArray.copyOf(): kotlin.IntArray

public fun kotlin.IntArray.copyOf(newSize: kotlin.Int): kotlin.IntArray

public fun kotlin.LongArray.copyOf(): kotlin.LongArray

public fun kotlin.LongArray.copyOf(newSize: kotlin.Int): kotlin.LongArray

public inline fun kotlin.ShortArray.copyOf(): kotlin.ShortArray

public fun kotlin.ShortArray.copyOf(newSize: kotlin.Int): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.copyOf(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.copyOf(newSize: kotlin.Int): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.copyOf(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.copyOf(newSize: kotlin.Int): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.copyOf(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.copyOf(newSize: kotlin.Int): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.copyOf(): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.copyOf(newSize: kotlin.Int): kotlin.UShortArray

public fun <T> kotlin.Array<out T>.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Array<T>

public fun kotlin.BooleanArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.BooleanArray

public fun kotlin.ByteArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.ByteArray

public fun kotlin.CharArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.CharArray

public fun kotlin.DoubleArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.DoubleArray

public fun kotlin.FloatArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.FloatArray

public fun kotlin.IntArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.IntArray

public fun kotlin.LongArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.LongArray

public fun kotlin.ShortArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.copyOfRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.UShortArray

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.count(): kotlin.Int

public inline fun <T> kotlin.Array<out T>.count(predicate: (T) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.count(): kotlin.Int

public inline fun kotlin.BooleanArray.count(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.count(): kotlin.Int

public inline fun kotlin.ByteArray.count(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.count(): kotlin.Int

public inline fun kotlin.CharArray.count(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.count(): kotlin.Int

public inline fun kotlin.DoubleArray.count(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.count(): kotlin.Int

public inline fun kotlin.FloatArray.count(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.count(): kotlin.Int

public inline fun kotlin.IntArray.count(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.count(): kotlin.Int

public inline fun kotlin.LongArray.count(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.count(): kotlin.Int

public inline fun kotlin.ShortArray.count(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.count(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.count(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.count(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.count(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.count(): kotlin.Int

public fun <T> kotlin.collections.Iterable<T>.count(): kotlin.Int

public inline fun <T> kotlin.collections.Iterable<T>.count(predicate: (T) -> kotlin.Boolean): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.count(): kotlin.Int

public inline fun <K, V> kotlin.collections.Map<out K, V>.count(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Int

public fun <T> kotlin.Array<out T>.distinct(): kotlin.collections.List<T>

public fun kotlin.BooleanArray.distinct(): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.distinct(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.distinct(): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.distinct(): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.distinct(): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.distinct(): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.distinct(): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.distinct(): kotlin.collections.List<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.distinct(): kotlin.collections.List<T>

public inline fun <T, K> kotlin.Array<out T>.distinctBy(selector: (T) -> K): kotlin.collections.List<T>

public inline fun <K> kotlin.BooleanArray.distinctBy(selector: (kotlin.Boolean) -> K): kotlin.collections.List<kotlin.Boolean>

public inline fun <K> kotlin.ByteArray.distinctBy(selector: (kotlin.Byte) -> K): kotlin.collections.List<kotlin.Byte>

public inline fun <K> kotlin.CharArray.distinctBy(selector: (kotlin.Char) -> K): kotlin.collections.List<kotlin.Char>

public inline fun <K> kotlin.DoubleArray.distinctBy(selector: (kotlin.Double) -> K): kotlin.collections.List<kotlin.Double>

public inline fun <K> kotlin.FloatArray.distinctBy(selector: (kotlin.Float) -> K): kotlin.collections.List<kotlin.Float>

public inline fun <K> kotlin.IntArray.distinctBy(selector: (kotlin.Int) -> K): kotlin.collections.List<kotlin.Int>

public inline fun <K> kotlin.LongArray.distinctBy(selector: (kotlin.Long) -> K): kotlin.collections.List<kotlin.Long>

public inline fun <K> kotlin.ShortArray.distinctBy(selector: (kotlin.Short) -> K): kotlin.collections.List<kotlin.Short>

public inline fun <T, K> kotlin.collections.Iterable<T>.distinctBy(selector: (T) -> K): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.drop(n: kotlin.Int): kotlin.collections.List<T>

public fun kotlin.BooleanArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.drop(n: kotlin.Int): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.Iterable<T>.drop(n: kotlin.Int): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.dropLast(n: kotlin.Int): kotlin.collections.List<T>

public fun kotlin.BooleanArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.dropLast(n: kotlin.Int): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.List<T>.dropLast(n: kotlin.Int): kotlin.collections.List<T>

public inline fun <T> kotlin.Array<out T>.dropLastWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.dropLastWhile(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.dropLastWhile(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.dropLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.dropLastWhile(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.dropLastWhile(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.dropLastWhile(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.dropLastWhile(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.dropLastWhile(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.dropLastWhile(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.dropLastWhile(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.dropLastWhile(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.dropLastWhile(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.List<T>.dropLastWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun <T> kotlin.Array<out T>.dropWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.dropWhile(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.dropWhile(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.dropWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.dropWhile(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.dropWhile(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.dropWhile(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.dropWhile(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.dropWhile(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.dropWhile(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.dropWhile(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.dropWhile(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.dropWhile(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.Iterable<T>.dropWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

@kotlin.SinceKotlin(version = "1.1")
public fun <T, K> kotlin.collections.Grouping<T, K>.eachCount(): kotlin.collections.Map<K, kotlin.Int>

@kotlin.SinceKotlin(version = "1.1")
public fun <T, K, M : kotlin.collections.MutableMap<in K, kotlin.Int>> kotlin.collections.Grouping<T, K>.eachCountTo(destination: M): M

public fun <T> kotlin.Array<out T>.elementAt(index: kotlin.Int): T

public fun kotlin.BooleanArray.elementAt(index: kotlin.Int): kotlin.Boolean

public fun kotlin.ByteArray.elementAt(index: kotlin.Int): kotlin.Byte

public fun kotlin.CharArray.elementAt(index: kotlin.Int): kotlin.Char

public fun kotlin.DoubleArray.elementAt(index: kotlin.Int): kotlin.Double

public fun kotlin.FloatArray.elementAt(index: kotlin.Int): kotlin.Float

public fun kotlin.IntArray.elementAt(index: kotlin.Int): kotlin.Int

public fun kotlin.LongArray.elementAt(index: kotlin.Int): kotlin.Long

public fun kotlin.ShortArray.elementAt(index: kotlin.Int): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.elementAt(index: kotlin.Int): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.elementAt(index: kotlin.Int): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.elementAt(index: kotlin.Int): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.elementAt(index: kotlin.Int): kotlin.UShort

public fun <T> kotlin.collections.Iterable<T>.elementAt(index: kotlin.Int): T

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>.elementAt(index: kotlin.Int): T

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Byte): kotlin.Byte

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Double): kotlin.Double

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Float): kotlin.Float

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Int): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Long): kotlin.Long

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UShort): kotlin.UShort

public fun <T> kotlin.collections.Iterable<T>.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.elementAtOrNull(index: kotlin.Int): T?

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.elementAtOrNull(index: kotlin.Int): kotlin.Boolean?

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.elementAtOrNull(index: kotlin.Int): kotlin.Byte?

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.elementAtOrNull(index: kotlin.Int): kotlin.Char?

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.elementAtOrNull(index: kotlin.Int): kotlin.Double?

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.elementAtOrNull(index: kotlin.Int): kotlin.Float?

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.elementAtOrNull(index: kotlin.Int): kotlin.Int?

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.elementAtOrNull(index: kotlin.Int): kotlin.Long?

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.elementAtOrNull(index: kotlin.Int): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.elementAtOrNull(index: kotlin.Int): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.elementAtOrNull(index: kotlin.Int): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.elementAtOrNull(index: kotlin.Int): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.elementAtOrNull(index: kotlin.Int): kotlin.UShort?

public fun <T> kotlin.collections.Iterable<T>.elementAtOrNull(index: kotlin.Int): T?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>.elementAtOrNull(index: kotlin.Int): T?

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.Array<T>.fill(element: T, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.BooleanArray.fill(element: kotlin.Boolean, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ByteArray.fill(element: kotlin.Byte, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.CharArray.fill(element: kotlin.Char, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.DoubleArray.fill(element: kotlin.Double, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.FloatArray.fill(element: kotlin.Float, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.IntArray.fill(element: kotlin.Int, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.LongArray.fill(element: kotlin.Long, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ShortArray.fill(element: kotlin.Short, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.fill(element: kotlin.UByte, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.fill(element: kotlin.UInt, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.fill(element: kotlin.ULong, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.fill(element: kotlin.UShort, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.MutableList<T>.fill(value: T): kotlin.Unit

public inline fun <T> kotlin.Array<out T>.filter(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.filter(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.filter(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.filter(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.filter(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.filter(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.filter(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.filter(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.filter(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.filter(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.filter(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.filter(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.filter(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.Iterable<T>.filter(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun <K, V> kotlin.collections.Map<out K, V>.filter(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.collections.Map<K, V>

public inline fun <T> kotlin.Array<out T>.filterIndexed(predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.filterIndexed(predicate: (index: kotlin.Int, kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.Iterable<T>.filterIndexed(predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Byte) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Double) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Float) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Int) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Long) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Short) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.UByte) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.UInt) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.ULong) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.UShort) -> kotlin.Boolean): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C

public inline fun <reified R> kotlin.Array<*>.filterIsInstance(): kotlin.collections.List<R>

public inline fun <reified R> kotlin.collections.Iterable<*>.filterIsInstance(): kotlin.collections.List<R>

public inline fun <reified R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<*>.filterIsInstanceTo(destination: C): C

public inline fun <reified R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<*>.filterIsInstanceTo(destination: C): C

public inline fun <K, V> kotlin.collections.Map<out K, V>.filterKeys(predicate: (K) -> kotlin.Boolean): kotlin.collections.Map<K, V>

public inline fun <T> kotlin.Array<out T>.filterNot(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.filterNot(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.filterNot(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.filterNot(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.filterNot(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.filterNot(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.filterNot(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.filterNot(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.filterNot(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.filterNot(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.filterNot(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.filterNot(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.filterNot(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.Iterable<T>.filterNot(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun <K, V> kotlin.collections.Map<out K, V>.filterNot(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.collections.Map<K, V>

public fun <T : kotlin.Any> kotlin.Array<out T?>.filterNotNull(): kotlin.collections.List<T>

public fun <T : kotlin.Any> kotlin.collections.Iterable<T?>.filterNotNull(): kotlin.collections.List<T>

public fun <C : kotlin.collections.MutableCollection<in T>, T : kotlin.Any> kotlin.Array<out T?>.filterNotNullTo(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in T>, T : kotlin.Any> kotlin.collections.Iterable<T?>.filterNotNullTo(destination: C): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterNotTo(destination: C, predicate: (T) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterNotTo(destination: C, predicate: (kotlin.Boolean) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterNotTo(destination: C, predicate: (kotlin.Byte) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterNotTo(destination: C, predicate: (kotlin.Char) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterNotTo(destination: C, predicate: (kotlin.Double) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterNotTo(destination: C, predicate: (kotlin.Float) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterNotTo(destination: C, predicate: (kotlin.Int) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterNotTo(destination: C, predicate: (kotlin.Long) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterNotTo(destination: C, predicate: (kotlin.Short) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterNotTo(destination: C, predicate: (kotlin.UByte) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterNotTo(destination: C, predicate: (kotlin.UInt) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterNotTo(destination: C, predicate: (kotlin.ULong) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterNotTo(destination: C, predicate: (kotlin.UShort) -> kotlin.Boolean): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterNotTo(destination: C, predicate: (T) -> kotlin.Boolean): C

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Map<out K, V>.filterNotTo(destination: M, predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): M

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.filterTo(destination: C, predicate: (T) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.filterTo(destination: C, predicate: (kotlin.Boolean) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.filterTo(destination: C, predicate: (kotlin.Byte) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.filterTo(destination: C, predicate: (kotlin.Char) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.filterTo(destination: C, predicate: (kotlin.Double) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.filterTo(destination: C, predicate: (kotlin.Float) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.filterTo(destination: C, predicate: (kotlin.Int) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.filterTo(destination: C, predicate: (kotlin.Long) -> kotlin.Boolean): C

public inline fun <C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.filterTo(destination: C, predicate: (kotlin.Short) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UByte>> kotlin.UByteArray.filterTo(destination: C, predicate: (kotlin.UByte) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UInt>> kotlin.UIntArray.filterTo(destination: C, predicate: (kotlin.UInt) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.ULong>> kotlin.ULongArray.filterTo(destination: C, predicate: (kotlin.ULong) -> kotlin.Boolean): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.MutableCollection<in kotlin.UShort>> kotlin.UShortArray.filterTo(destination: C, predicate: (kotlin.UShort) -> kotlin.Boolean): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.filterTo(destination: C, predicate: (T) -> kotlin.Boolean): C

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Map<out K, V>.filterTo(destination: M, predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): M

public inline fun <K, V> kotlin.collections.Map<out K, V>.filterValues(predicate: (V) -> kotlin.Boolean): kotlin.collections.Map<K, V>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.find(predicate: (T) -> kotlin.Boolean): T?

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.find(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.find(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.find(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.find(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.find(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.find(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.find(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.find(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.find(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.find(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.find(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.find(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.find(predicate: (T) -> kotlin.Boolean): T?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.findLast(predicate: (T) -> kotlin.Boolean): T?

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.findLast(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.findLast(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.findLast(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.findLast(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.findLast(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.findLast(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.findLast(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.findLast(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.findLast(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.findLast(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.findLast(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.findLast(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.findLast(predicate: (T) -> kotlin.Boolean): T?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>.findLast(predicate: (T) -> kotlin.Boolean): T?

public fun <T> kotlin.Array<out T>.first(): T

public inline fun <T> kotlin.Array<out T>.first(predicate: (T) -> kotlin.Boolean): T

public fun kotlin.BooleanArray.first(): kotlin.Boolean

public inline fun kotlin.BooleanArray.first(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ByteArray.first(): kotlin.Byte

public inline fun kotlin.ByteArray.first(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte

public fun kotlin.CharArray.first(): kotlin.Char

public inline fun kotlin.CharArray.first(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

public fun kotlin.DoubleArray.first(): kotlin.Double

public inline fun kotlin.DoubleArray.first(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double

public fun kotlin.FloatArray.first(): kotlin.Float

public inline fun kotlin.FloatArray.first(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float

public fun kotlin.IntArray.first(): kotlin.Int

public inline fun kotlin.IntArray.first(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

public fun kotlin.LongArray.first(): kotlin.Long

public inline fun kotlin.LongArray.first(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long

public fun kotlin.ShortArray.first(): kotlin.Short

public inline fun kotlin.ShortArray.first(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.first(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.first(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.first(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.first(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.first(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.first(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.first(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.first(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort

public fun <T> kotlin.collections.Iterable<T>.first(): T

public inline fun <T> kotlin.collections.Iterable<T>.first(predicate: (T) -> kotlin.Boolean): T

public fun <T> kotlin.collections.List<T>.first(): T

public fun <T> kotlin.Array<out T>.firstOrNull(): T?

public inline fun <T> kotlin.Array<out T>.firstOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun kotlin.BooleanArray.firstOrNull(): kotlin.Boolean?

public inline fun kotlin.BooleanArray.firstOrNull(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

public fun kotlin.ByteArray.firstOrNull(): kotlin.Byte?

public inline fun kotlin.ByteArray.firstOrNull(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?

public fun kotlin.CharArray.firstOrNull(): kotlin.Char?

public inline fun kotlin.CharArray.firstOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.DoubleArray.firstOrNull(): kotlin.Double?

public inline fun kotlin.DoubleArray.firstOrNull(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?

public fun kotlin.FloatArray.firstOrNull(): kotlin.Float?

public inline fun kotlin.FloatArray.firstOrNull(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?

public fun kotlin.IntArray.firstOrNull(): kotlin.Int?

public inline fun kotlin.IntArray.firstOrNull(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?

public fun kotlin.LongArray.firstOrNull(): kotlin.Long?

public inline fun kotlin.LongArray.firstOrNull(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?

public fun kotlin.ShortArray.firstOrNull(): kotlin.Short?

public inline fun kotlin.ShortArray.firstOrNull(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.firstOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.firstOrNull(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.firstOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.firstOrNull(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.firstOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.firstOrNull(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.firstOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.firstOrNull(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?

public fun <T> kotlin.collections.Iterable<T>.firstOrNull(): T?

public inline fun <T> kotlin.collections.Iterable<T>.firstOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun <T> kotlin.collections.List<T>.firstOrNull(): T?

public inline fun <T, R> kotlin.Array<out T>.flatMap(transform: (T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequence")
public inline fun <T, R> kotlin.Array<out T>.flatMap(transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.BooleanArray.flatMap(transform: (kotlin.Boolean) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.ByteArray.flatMap(transform: (kotlin.Byte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.CharArray.flatMap(transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.DoubleArray.flatMap(transform: (kotlin.Double) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.FloatArray.flatMap(transform: (kotlin.Float) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.IntArray.flatMap(transform: (kotlin.Int) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.LongArray.flatMap(transform: (kotlin.Long) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <R> kotlin.ShortArray.flatMap(transform: (kotlin.Short) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.flatMap(transform: (kotlin.UByte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.flatMap(transform: (kotlin.UInt) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.flatMap(transform: (kotlin.ULong) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.flatMap(transform: (kotlin.UShort) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

public inline fun <T, R> kotlin.collections.Iterable<T>.flatMap(transform: (T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequence")
public inline fun <T, R> kotlin.collections.Iterable<T>.flatMap(transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>

public inline fun <K, V, R> kotlin.collections.Map<out K, V>.flatMap(transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequence")
public inline fun <K, V, R> kotlin.collections.Map<out K, V>.flatMap(transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequence")
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Boolean) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Byte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Double) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Float) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Int) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Long) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Short) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.UByte) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.UInt) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.ULong) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.flatMapIndexed(transform: (index: kotlin.Int, kotlin.UShort) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequence")
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequenceTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Boolean) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Byte) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Char) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Double) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Float) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Int) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Long) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Short) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UByte) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UInt) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.ULong) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UShort) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequenceTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapTo(destination: C, transform: (T) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequenceTo")
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.flatMapTo(destination: C, transform: (T) -> kotlin.sequences.Sequence<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.flatMapTo(destination: C, transform: (kotlin.Boolean) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.flatMapTo(destination: C, transform: (kotlin.Byte) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.flatMapTo(destination: C, transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.flatMapTo(destination: C, transform: (kotlin.Double) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.flatMapTo(destination: C, transform: (kotlin.Float) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.flatMapTo(destination: C, transform: (kotlin.Int) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.flatMapTo(destination: C, transform: (kotlin.Long) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.flatMapTo(destination: C, transform: (kotlin.Short) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.flatMapTo(destination: C, transform: (kotlin.UByte) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.flatMapTo(destination: C, transform: (kotlin.UInt) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.flatMapTo(destination: C, transform: (kotlin.ULong) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.flatMapTo(destination: C, transform: (kotlin.UShort) -> kotlin.collections.Iterable<R>): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapTo(destination: C, transform: (T) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequenceTo")
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.flatMapTo(destination: C, transform: (T) -> kotlin.sequences.Sequence<R>): C

public inline fun <K, V, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.flatMapTo(destination: C, transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapSequenceTo")
public inline fun <K, V, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.flatMapTo(destination: C, transform: (kotlin.collections.Map.Entry<K, V>) -> kotlin.sequences.Sequence<R>): C

public fun <T> kotlin.Array<out kotlin.Array<out T>>.flatten(): kotlin.collections.List<T>

public fun <T> kotlin.collections.Iterable<kotlin.collections.Iterable<T>>.flatten(): kotlin.collections.List<T>

public inline fun <T, R> kotlin.Array<out T>.fold(initial: R, operation: (acc: R, T) -> R): R

public inline fun <R> kotlin.BooleanArray.fold(initial: R, operation: (acc: R, kotlin.Boolean) -> R): R

public inline fun <R> kotlin.ByteArray.fold(initial: R, operation: (acc: R, kotlin.Byte) -> R): R

public inline fun <R> kotlin.CharArray.fold(initial: R, operation: (acc: R, kotlin.Char) -> R): R

public inline fun <R> kotlin.DoubleArray.fold(initial: R, operation: (acc: R, kotlin.Double) -> R): R

public inline fun <R> kotlin.FloatArray.fold(initial: R, operation: (acc: R, kotlin.Float) -> R): R

public inline fun <R> kotlin.IntArray.fold(initial: R, operation: (acc: R, kotlin.Int) -> R): R

public inline fun <R> kotlin.LongArray.fold(initial: R, operation: (acc: R, kotlin.Long) -> R): R

public inline fun <R> kotlin.ShortArray.fold(initial: R, operation: (acc: R, kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.fold(initial: R, operation: (acc: R, kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.fold(initial: R, operation: (acc: R, kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.fold(initial: R, operation: (acc: R, kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.fold(initial: R, operation: (acc: R, kotlin.UShort) -> R): R

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R> kotlin.collections.Grouping<T, K>.fold(initialValueSelector: (key: K, element: T) -> R, operation: (key: K, accumulator: R, element: T) -> R): kotlin.collections.Map<K, R>

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R> kotlin.collections.Grouping<T, K>.fold(initialValue: R, operation: (accumulator: R, element: T) -> R): kotlin.collections.Map<K, R>

public inline fun <T, R> kotlin.collections.Iterable<T>.fold(initial: R, operation: (acc: R, T) -> R): R

public inline fun <T, R> kotlin.Array<out T>.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): R

public inline fun <R> kotlin.BooleanArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Boolean) -> R): R

public inline fun <R> kotlin.ByteArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Byte) -> R): R

public inline fun <R> kotlin.CharArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): R

public inline fun <R> kotlin.DoubleArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Double) -> R): R

public inline fun <R> kotlin.FloatArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Float) -> R): R

public inline fun <R> kotlin.IntArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Int) -> R): R

public inline fun <R> kotlin.LongArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Long) -> R): R

public inline fun <R> kotlin.ShortArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UShort) -> R): R

public inline fun <T, R> kotlin.collections.Iterable<T>.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): R

public inline fun <T, R> kotlin.Array<out T>.foldRight(initial: R, operation: (T, acc: R) -> R): R

public inline fun <R> kotlin.BooleanArray.foldRight(initial: R, operation: (kotlin.Boolean, acc: R) -> R): R

public inline fun <R> kotlin.ByteArray.foldRight(initial: R, operation: (kotlin.Byte, acc: R) -> R): R

public inline fun <R> kotlin.CharArray.foldRight(initial: R, operation: (kotlin.Char, acc: R) -> R): R

public inline fun <R> kotlin.DoubleArray.foldRight(initial: R, operation: (kotlin.Double, acc: R) -> R): R

public inline fun <R> kotlin.FloatArray.foldRight(initial: R, operation: (kotlin.Float, acc: R) -> R): R

public inline fun <R> kotlin.IntArray.foldRight(initial: R, operation: (kotlin.Int, acc: R) -> R): R

public inline fun <R> kotlin.LongArray.foldRight(initial: R, operation: (kotlin.Long, acc: R) -> R): R

public inline fun <R> kotlin.ShortArray.foldRight(initial: R, operation: (kotlin.Short, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.foldRight(initial: R, operation: (kotlin.UByte, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.foldRight(initial: R, operation: (kotlin.UInt, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.foldRight(initial: R, operation: (kotlin.ULong, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.foldRight(initial: R, operation: (kotlin.UShort, acc: R) -> R): R

public inline fun <T, R> kotlin.collections.List<T>.foldRight(initial: R, operation: (T, acc: R) -> R): R

public inline fun <T, R> kotlin.Array<out T>.foldRightIndexed(initial: R, operation: (index: kotlin.Int, T, acc: R) -> R): R

public inline fun <R> kotlin.BooleanArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Boolean, acc: R) -> R): R

public inline fun <R> kotlin.ByteArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Byte, acc: R) -> R): R

public inline fun <R> kotlin.CharArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Char, acc: R) -> R): R

public inline fun <R> kotlin.DoubleArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Double, acc: R) -> R): R

public inline fun <R> kotlin.FloatArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Float, acc: R) -> R): R

public inline fun <R> kotlin.IntArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Int, acc: R) -> R): R

public inline fun <R> kotlin.LongArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Long, acc: R) -> R): R

public inline fun <R> kotlin.ShortArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Short, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.UByte, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.UInt, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.ULong, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.UShort, acc: R) -> R): R

public inline fun <T, R> kotlin.collections.List<T>.foldRightIndexed(initial: R, operation: (index: kotlin.Int, T, acc: R) -> R): R

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R, M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.foldTo(destination: M, initialValueSelector: (key: K, element: T) -> R, operation: (key: K, accumulator: R, element: T) -> R): M

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K, R, M : kotlin.collections.MutableMap<in K, R>> kotlin.collections.Grouping<T, K>.foldTo(destination: M, initialValue: R, operation: (accumulator: R, element: T) -> R): M

public inline fun <T> kotlin.Array<out T>.forEach(action: (T) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.BooleanArray.forEach(action: (kotlin.Boolean) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.ByteArray.forEach(action: (kotlin.Byte) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.CharArray.forEach(action: (kotlin.Char) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.DoubleArray.forEach(action: (kotlin.Double) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.FloatArray.forEach(action: (kotlin.Float) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.IntArray.forEach(action: (kotlin.Int) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.LongArray.forEach(action: (kotlin.Long) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.ShortArray.forEach(action: (kotlin.Short) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.forEach(action: (kotlin.UByte) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.forEach(action: (kotlin.UInt) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.forEach(action: (kotlin.ULong) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.forEach(action: (kotlin.UShort) -> kotlin.Unit): kotlin.Unit

@kotlin.internal.HidesMembers
public inline fun <T> kotlin.collections.Iterable<T>.forEach(action: (T) -> kotlin.Unit): kotlin.Unit

public inline fun <T> kotlin.collections.Iterator<T>.forEach(operation: (T) -> kotlin.Unit): kotlin.Unit

@kotlin.internal.HidesMembers
public inline fun <K, V> kotlin.collections.Map<out K, V>.forEach(action: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): kotlin.Unit

public inline fun <T> kotlin.Array<out T>.forEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.BooleanArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.ByteArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Byte) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.CharArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.DoubleArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Double) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.FloatArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Float) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.IntArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Int) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.LongArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Long) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.ShortArray.forEachIndexed(action: (index: kotlin.Int, kotlin.Short) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.forEachIndexed(action: (index: kotlin.Int, kotlin.UByte) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.forEachIndexed(action: (index: kotlin.Int, kotlin.UInt) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.forEachIndexed(action: (index: kotlin.Int, kotlin.ULong) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.forEachIndexed(action: (index: kotlin.Int, kotlin.UShort) -> kotlin.Unit): kotlin.Unit

public inline fun <T> kotlin.collections.Iterable<T>.forEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes
K, V> kotlin.collections.Map<out K, V>.get(key: K): V?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Byte): kotlin.Byte

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Double): kotlin.Double

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Float): kotlin.Float

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Int): kotlin.Int

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Long): kotlin.Long

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.UShort): kotlin.UShort

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V

public fun <T> kotlin.Array<out T>.getOrNull(index: kotlin.Int): T?

public fun kotlin.BooleanArray.getOrNull(index: kotlin.Int): kotlin.Boolean?

public fun kotlin.ByteArray.getOrNull(index: kotlin.Int): kotlin.Byte?

public fun kotlin.CharArray.getOrNull(index: kotlin.Int): kotlin.Char?

public fun kotlin.DoubleArray.getOrNull(index: kotlin.Int): kotlin.Double?

public fun kotlin.FloatArray.getOrNull(index: kotlin.Int): kotlin.Float?

public fun kotlin.IntArray.getOrNull(index: kotlin.Int): kotlin.Int?

public fun kotlin.LongArray.getOrNull(index: kotlin.Int): kotlin.Long?

public fun kotlin.ShortArray.getOrNull(index: kotlin.Int): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.getOrNull(index: kotlin.Int): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.getOrNull(index: kotlin.Int): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.getOrNull(index: kotlin.Int): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.getOrNull(index: kotlin.Int): kotlin.UShort?

public fun <T> kotlin.collections.List<T>.getOrNull(index: kotlin.Int): T?

public inline fun <K, V> kotlin.collections.MutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V

@kotlin.SinceKotlin(version = "1.1")
public fun <K, V> kotlin.collections.Map<K, V>.getValue(key: K): V

@kotlin.internal.InlineOnly
public inline operator fun <V, V1 : V> kotlin.collections.Map<in kotlin.String, V>.getValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): V1

@kotlin.jvm.JvmName(name = "getVar")
@kotlin.internal.InlineOnly
public inline operator fun <V, V1 : V> kotlin.collections.MutableMap<in kotlin.String, out V>.getValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): V1

public inline fun <T, K> kotlin.Array<out T>.groupBy(keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>

public inline fun <T, K, V> kotlin.Array<out T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.BooleanArray.groupBy(keySelector: (kotlin.Boolean) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Boolean>>

public inline fun <K, V> kotlin.BooleanArray.groupBy(keySelector: (kotlin.Boolean) -> K, valueTransform: (kotlin.Boolean) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.ByteArray.groupBy(keySelector: (kotlin.Byte) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Byte>>

public inline fun <K, V> kotlin.ByteArray.groupBy(keySelector: (kotlin.Byte) -> K, valueTransform: (kotlin.Byte) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.CharArray.groupBy(keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Char>>

public inline fun <K, V> kotlin.CharArray.groupBy(keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.DoubleArray.groupBy(keySelector: (kotlin.Double) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Double>>

public inline fun <K, V> kotlin.DoubleArray.groupBy(keySelector: (kotlin.Double) -> K, valueTransform: (kotlin.Double) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.FloatArray.groupBy(keySelector: (kotlin.Float) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Float>>

public inline fun <K, V> kotlin.FloatArray.groupBy(keySelector: (kotlin.Float) -> K, valueTransform: (kotlin.Float) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.IntArray.groupBy(keySelector: (kotlin.Int) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Int>>

public inline fun <K, V> kotlin.IntArray.groupBy(keySelector: (kotlin.Int) -> K, valueTransform: (kotlin.Int) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.LongArray.groupBy(keySelector: (kotlin.Long) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Long>>

public inline fun <K, V> kotlin.LongArray.groupBy(keySelector: (kotlin.Long) -> K, valueTransform: (kotlin.Long) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K> kotlin.ShortArray.groupBy(keySelector: (kotlin.Short) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Short>>

public inline fun <K, V> kotlin.ShortArray.groupBy(keySelector: (kotlin.Short) -> K, valueTransform: (kotlin.Short) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K> kotlin.UByteArray.groupBy(keySelector: (kotlin.UByte) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UByte>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.UByteArray.groupBy(keySelector: (kotlin.UByte) -> K, valueTransform: (kotlin.UByte) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K> kotlin.UIntArray.groupBy(keySelector: (kotlin.UInt) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UInt>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.UIntArray.groupBy(keySelector: (kotlin.UInt) -> K, valueTransform: (kotlin.UInt) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K> kotlin.ULongArray.groupBy(keySelector: (kotlin.ULong) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.ULong>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.ULongArray.groupBy(keySelector: (kotlin.ULong) -> K, valueTransform: (kotlin.ULong) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K> kotlin.UShortArray.groupBy(keySelector: (kotlin.UShort) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UShort>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.UShortArray.groupBy(keySelector: (kotlin.UShort) -> K, valueTransform: (kotlin.UShort) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <T, K> kotlin.collections.Iterable<T>.groupBy(keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>

public inline fun <T, K, V> kotlin.collections.Iterable<T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.Array<out T>.groupByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.Array<out T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Boolean>>> kotlin.BooleanArray.groupByTo(destination: M, keySelector: (kotlin.Boolean) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.BooleanArray.groupByTo(destination: M, keySelector: (kotlin.Boolean) -> K, valueTransform: (kotlin.Boolean) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Byte>>> kotlin.ByteArray.groupByTo(destination: M, keySelector: (kotlin.Byte) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ByteArray.groupByTo(destination: M, keySelector: (kotlin.Byte) -> K, valueTransform: (kotlin.Byte) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Char>>> kotlin.CharArray.groupByTo(destination: M, keySelector: (kotlin.Char) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.CharArray.groupByTo(destination: M, keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Double>>> kotlin.DoubleArray.groupByTo(destination: M, keySelector: (kotlin.Double) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.DoubleArray.groupByTo(destination: M, keySelector: (kotlin.Double) -> K, valueTransform: (kotlin.Double) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Float>>> kotlin.FloatArray.groupByTo(destination: M, keySelector: (kotlin.Float) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.FloatArray.groupByTo(destination: M, keySelector: (kotlin.Float) -> K, valueTransform: (kotlin.Float) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Int>>> kotlin.IntArray.groupByTo(destination: M, keySelector: (kotlin.Int) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.IntArray.groupByTo(destination: M, keySelector: (kotlin.Int) -> K, valueTransform: (kotlin.Int) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Long>>> kotlin.LongArray.groupByTo(destination: M, keySelector: (kotlin.Long) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.LongArray.groupByTo(destination: M, keySelector: (kotlin.Long) -> K, valueTransform: (kotlin.Long) -> V): M

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Short>>> kotlin.ShortArray.groupByTo(destination: M, keySelector: (kotlin.Short) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ShortArray.groupByTo(destination: M, keySelector: (kotlin.Short) -> K, valueTransform: (kotlin.Short) -> V): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UByte>>> kotlin.UByteArray.groupByTo(destination: M, keySelector: (kotlin.UByte) -> K): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UByteArray.groupByTo(destination: M, keySelector: (kotlin.UByte) -> K, valueTransform: (kotlin.UByte) -> V): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UInt>>> kotlin.UIntArray.groupByTo(destination: M, keySelector: (kotlin.UInt) -> K): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UIntArray.groupByTo(destination: M, keySelector: (kotlin.UInt) -> K, valueTransform: (kotlin.UInt) -> V): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.ULong>>> kotlin.ULongArray.groupByTo(destination: M, keySelector: (kotlin.ULong) -> K): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ULongArray.groupByTo(destination: M, keySelector: (kotlin.ULong) -> K, valueTransform: (kotlin.ULong) -> V): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UShort>>> kotlin.UShortArray.groupByTo(destination: M, keySelector: (kotlin.UShort) -> K): M

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UShortArray.groupByTo(destination: M, keySelector: (kotlin.UShort) -> K, valueTransform: (kotlin.UShort) -> V): M

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.collections.Iterable<T>.groupByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.collections.Iterable<T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K> kotlin.Array<out T>.groupingBy(crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K> kotlin.collections.Iterable<T>.groupingBy(crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.Array<*>, R> C.ifEmpty(defaultValue: () -> R): R where C : R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.collections.Collection<*>, R> C.ifEmpty(defaultValue: () -> R): R where C : R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <M : kotlin.collections.Map<*, *>, R> M.ifEmpty(defaultValue: () -> R): R where M : R

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.Array<out T>.indexOf(element: T): kotlin.Int

public fun kotlin.BooleanArray.indexOf(element: kotlin.Boolean): kotlin.Int

public fun kotlin.ByteArray.indexOf(element: kotlin.Byte): kotlin.Int

public fun kotlin.CharArray.indexOf(element: kotlin.Char): kotlin.Int

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'indexOfFirst { it == element }' instead to continue using this behavior, or '.asList().indexOf(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "indexOfFirst { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.indexOf(element: kotlin.Double): kotlin.Int

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'indexOfFirst { it == element }' instead to continue using this behavior, or '.asList().indexOf(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "indexOfFirst { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.indexOf(element: kotlin.Float): kotlin.Int

public fun kotlin.IntArray.indexOf(element: kotlin.Int): kotlin.Int

public fun kotlin.LongArray.indexOf(element: kotlin.Long): kotlin.Int

public fun kotlin.ShortArray.indexOf(element: kotlin.Short): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.indexOf(element: kotlin.UByte): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.indexOf(element: kotlin.UInt): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.indexOf(element: kotlin.ULong): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.indexOf(element: kotlin.UShort): kotlin.Int

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.Iterable<T>.indexOf(element: T): kotlin.Int

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.List<T>.indexOf(element: T): kotlin.Int

public inline fun <T> kotlin.Array<out T>.indexOfFirst(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.BooleanArray.indexOfFirst(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.ByteArray.indexOfFirst(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.CharArray.indexOfFirst(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.DoubleArray.indexOfFirst(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.FloatArray.indexOfFirst(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.IntArray.indexOfFirst(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.LongArray.indexOfFirst(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.ShortArray.indexOfFirst(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.indexOfFirst(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.indexOfFirst(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.indexOfFirst(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.indexOfFirst(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.collections.Iterable<T>.indexOfFirst(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.collections.List<T>.indexOfFirst(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.Array<out T>.indexOfLast(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.BooleanArray.indexOfLast(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.ByteArray.indexOfLast(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.CharArray.indexOfLast(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.DoubleArray.indexOfLast(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.FloatArray.indexOfLast(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.IntArray.indexOfLast(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.LongArray.indexOfLast(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.ShortArray.indexOfLast(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.indexOfLast(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.indexOfLast(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.indexOfLast(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.indexOfLast(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.collections.Iterable<T>.indexOfLast(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.collections.List<T>.indexOfLast(predicate: (T) -> kotlin.Boolean): kotlin.Int

public infix fun <T> kotlin.Array<out T>.intersect(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public infix fun kotlin.BooleanArray.intersect(other: kotlin.collections.Iterable<kotlin.Boolean>): kotlin.collections.Set<kotlin.Boolean>

public infix fun kotlin.ByteArray.intersect(other: kotlin.collections.Iterable<kotlin.Byte>): kotlin.collections.Set<kotlin.Byte>

public infix fun kotlin.CharArray.intersect(other: kotlin.collections.Iterable<kotlin.Char>): kotlin.collections.Set<kotlin.Char>

public infix fun kotlin.DoubleArray.intersect(other: kotlin.collections.Iterable<kotlin.Double>): kotlin.collections.Set<kotlin.Double>

public infix fun kotlin.FloatArray.intersect(other: kotlin.collections.Iterable<kotlin.Float>): kotlin.collections.Set<kotlin.Float>

public infix fun kotlin.IntArray.intersect(other: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.Set<kotlin.Int>

public infix fun kotlin.LongArray.intersect(other: kotlin.collections.Iterable<kotlin.Long>): kotlin.collections.Set<kotlin.Long>

public infix fun kotlin.ShortArray.intersect(other: kotlin.collections.Iterable<kotlin.Short>): kotlin.collections.Set<kotlin.Short>

public infix fun <T> kotlin.collections.Iterable<T>.intersect(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.isEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.isNotEmpty(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.Array<*>?.isNullOrEmpty(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>?.isNullOrEmpty(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>?.isNullOrEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.Iterator<T>.iterator(): kotlin.collections.Iterator<T>

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.Map<out K, V>.iterator(): kotlin.collections.Iterator<kotlin.collections.Map.Entry<K, V>>

@kotlin.jvm.JvmName(name = "mutableIterator")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.iterator(): kotlin.collections.MutableIterator<kotlin.collections.MutableMap.MutableEntry<K, V>>

public fun <T, A : kotlin.text.Appendable> kotlin.Array<out T>.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.BooleanArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Boolean) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.ByteArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Byte) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.CharArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Char) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.DoubleArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Double) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.FloatArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Float) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.IntArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Int) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.LongArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Long) -> kotlin.CharSequence)? = ...): A

public fun <A : kotlin.text.Appendable> kotlin.ShortArray.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Short) -> kotlin.CharSequence)? = ...): A

public fun <T, A : kotlin.text.Appendable> kotlin.collections.Iterable<T>.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): A

public fun <T> kotlin.Array<out T>.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.BooleanArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Boolean) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.ByteArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Byte) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.CharArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Char) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.DoubleArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Double) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.FloatArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Float) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.IntArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Int) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.LongArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Long) -> kotlin.CharSequence)? = ...): kotlin.String

public fun kotlin.ShortArray.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((kotlin.Short) -> kotlin.CharSequence)? = ...): kotlin.String

public fun <T> kotlin.collections.Iterable<T>.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String

public fun <T> kotlin.Array<out T>.last(): T

public inline fun <T> kotlin.Array<out T>.last(predicate: (T) -> kotlin.Boolean): T

public fun kotlin.BooleanArray.last(): kotlin.Boolean

public inline fun kotlin.BooleanArray.last(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ByteArray.last(): kotlin.Byte

public inline fun kotlin.ByteArray.last(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte

public fun kotlin.CharArray.last(): kotlin.Char

public inline fun kotlin.CharArray.last(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

public fun kotlin.DoubleArray.last(): kotlin.Double

public inline fun kotlin.DoubleArray.last(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double

public fun kotlin.FloatArray.last(): kotlin.Float

public inline fun kotlin.FloatArray.last(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float

public fun kotlin.IntArray.last(): kotlin.Int

public inline fun kotlin.IntArray.last(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

public fun kotlin.LongArray.last(): kotlin.Long

public inline fun kotlin.LongArray.last(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long

public fun kotlin.ShortArray.last(): kotlin.Short

public inline fun kotlin.ShortArray.last(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.last(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.last(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.last(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.last(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.last(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.last(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.last(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.last(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort

public fun <T> kotlin.collections.Iterable<T>.last(): T

public inline fun <T> kotlin.collections.Iterable<T>.last(predicate: (T) -> kotlin.Boolean): T

public fun <T> kotlin.collections.List<T>.last(): T

public inline fun <T> kotlin.collections.List<T>.last(predicate: (T) -> kotlin.Boolean): T

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.Array<out T>.lastIndexOf(element: T): kotlin.Int

public fun kotlin.BooleanArray.lastIndexOf(element: kotlin.Boolean): kotlin.Int

public fun kotlin.ByteArray.lastIndexOf(element: kotlin.Byte): kotlin.Int

public fun kotlin.CharArray.lastIndexOf(element: kotlin.Char): kotlin.Int

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'indexOfLast { it == element }' instead to continue using this behavior, or '.asList().lastIndexOf(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "indexOfLast { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.lastIndexOf(element: kotlin.Double): kotlin.Int

@kotlin.Deprecated(message = "The function has unclear behavior when searching for NaN or zero values and will be removed soon. Use 'indexOfLast { it == element }' instead to continue using this behavior, or '.asList().lastIndexOf(element: T)' to get the same search behavior as in a list.", replaceWith = kotlin.ReplaceWith(expression = "indexOfLast { it == element }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.lastIndexOf(element: kotlin.Float): kotlin.Int

public fun kotlin.IntArray.lastIndexOf(element: kotlin.Int): kotlin.Int

public fun kotlin.LongArray.lastIndexOf(element: kotlin.Long): kotlin.Int

public fun kotlin.ShortArray.lastIndexOf(element: kotlin.Short): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.lastIndexOf(element: kotlin.UByte): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.lastIndexOf(element: kotlin.UInt): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.lastIndexOf(element: kotlin.ULong): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.lastIndexOf(element: kotlin.UShort): kotlin.Int

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.Iterable<T>.lastIndexOf(element: T): kotlin.Int

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.List<T>.lastIndexOf(element: T): kotlin.Int

public fun <T> kotlin.Array<out T>.lastOrNull(): T?

public inline fun <T> kotlin.Array<out T>.lastOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun kotlin.BooleanArray.lastOrNull(): kotlin.Boolean?

public inline fun kotlin.BooleanArray.lastOrNull(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

public fun kotlin.ByteArray.lastOrNull(): kotlin.Byte?

public inline fun kotlin.ByteArray.lastOrNull(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?

public fun kotlin.CharArray.lastOrNull(): kotlin.Char?

public inline fun kotlin.CharArray.lastOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.DoubleArray.lastOrNull(): kotlin.Double?

public inline fun kotlin.DoubleArray.lastOrNull(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?

public fun kotlin.FloatArray.lastOrNull(): kotlin.Float?

public inline fun kotlin.FloatArray.lastOrNull(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?

public fun kotlin.IntArray.lastOrNull(): kotlin.Int?

public inline fun kotlin.IntArray.lastOrNull(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?

public fun kotlin.LongArray.lastOrNull(): kotlin.Long?

public inline fun kotlin.LongArray.lastOrNull(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?

public fun kotlin.ShortArray.lastOrNull(): kotlin.Short?

public inline fun kotlin.ShortArray.lastOrNull(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.lastOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.lastOrNull(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.lastOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.lastOrNull(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.lastOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.lastOrNull(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.lastOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.lastOrNull(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?

public fun <T> kotlin.collections.Iterable<T>.lastOrNull(): T?

public inline fun <T> kotlin.collections.Iterable<T>.lastOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun <T> kotlin.collections.List<T>.lastOrNull(): T?

public inline fun <T> kotlin.collections.List<T>.lastOrNull(predicate: (T) -> kotlin.Boolean): T?

public inline fun <T, R> kotlin.Array<out T>.map(transform: (T) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.BooleanArray.map(transform: (kotlin.Boolean) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.ByteArray.map(transform: (kotlin.Byte) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.CharArray.map(transform: (kotlin.Char) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.DoubleArray.map(transform: (kotlin.Double) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.FloatArray.map(transform: (kotlin.Float) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.IntArray.map(transform: (kotlin.Int) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.LongArray.map(transform: (kotlin.Long) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.ShortArray.map(transform: (kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.map(transform: (kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.map(transform: (kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.map(transform: (kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.map(transform: (kotlin.UShort) -> R): kotlin.collections.List<R>

public inline fun <T, R> kotlin.collections.Iterable<T>.map(transform: (T) -> R): kotlin.collections.List<R>

public inline fun <K, V, R> kotlin.collections.Map<out K, V>.map(transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.List<R>

public inline fun <T, R> kotlin.Array<out T>.mapIndexed(transform: (index: kotlin.Int, T) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.BooleanArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Boolean) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.ByteArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Byte) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.CharArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Char) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.DoubleArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Double) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.FloatArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Float) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.IntArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Int) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.LongArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Long) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.ShortArray.mapIndexed(transform: (index: kotlin.Int, kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.mapIndexed(transform: (index: kotlin.Int, kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.mapIndexed(transform: (index: kotlin.Int, kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.mapIndexed(transform: (index: kotlin.Int, kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.mapIndexed(transform: (index: kotlin.Int, kotlin.UShort) -> R): kotlin.collections.List<R>

public inline fun <T, R> kotlin.collections.Iterable<T>.mapIndexed(transform: (index: kotlin.Int, T) -> R): kotlin.collections.List<R>

public inline fun <T, R : kotlin.Any> kotlin.Array<out T>.mapIndexedNotNull(transform: (index: kotlin.Int, T) -> R?): kotlin.collections.List<R>

public inline fun <T, R : kotlin.Any> kotlin.collections.Iterable<T>.mapIndexedNotNull(transform: (index: kotlin.Int, T) -> R?): kotlin.collections.List<R>

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapIndexedNotNullTo(destination: C, transform: (index: kotlin.Int, T) -> R?): C

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapIndexedNotNullTo(destination: C, transform: (index: kotlin.Int, T) -> R?): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Boolean) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Byte) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Char) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Double) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Float) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Int) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Long) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Short) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UByte) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UInt) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.ULong) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.UShort) -> R): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> R): C

public inline fun <K, V, R> kotlin.collections.Map<out K, V>.mapKeys(transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map<R, V>

public inline fun <K, V, R, M : kotlin.collections.MutableMap<in R, in V>> kotlin.collections.Map<out K, V>.mapKeysTo(destination: M, transform: (kotlin.collections.Map.Entry<K, V>) -> R): M

public inline fun <T, R : kotlin.Any> kotlin.Array<out T>.mapNotNull(transform: (T) -> R?): kotlin.collections.List<R>

public inline fun <T, R : kotlin.Any> kotlin.collections.Iterable<T>.mapNotNull(transform: (T) -> R?): kotlin.collections.List<R>

public inline fun <K, V, R : kotlin.Any> kotlin.collections.Map<out K, V>.mapNotNull(transform: (kotlin.collections.Map.Entry<K, V>) -> R?): kotlin.collections.List<R>

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapNotNullTo(destination: C, transform: (T) -> R?): C

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapNotNullTo(destination: C, transform: (T) -> R?): C

public inline fun <K, V, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.mapNotNullTo(destination: C, transform: (kotlin.collections.Map.Entry<K, V>) -> R?): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapTo(destination: C, transform: (T) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.mapTo(destination: C, transform: (kotlin.Boolean) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.mapTo(destination: C, transform: (kotlin.Byte) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.mapTo(destination: C, transform: (kotlin.Char) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.mapTo(destination: C, transform: (kotlin.Double) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.mapTo(destination: C, transform: (kotlin.Float) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.mapTo(destination: C, transform: (kotlin.Int) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.mapTo(destination: C, transform: (kotlin.Long) -> R): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.mapTo(destination: C, transform: (kotlin.Short) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.mapTo(destination: C, transform: (kotlin.UByte) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.mapTo(destination: C, transform: (kotlin.UInt) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.mapTo(destination: C, transform: (kotlin.ULong) -> R): C

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.mapTo(destination: C, transform: (kotlin.UShort) -> R): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapTo(destination: C, transform: (T) -> R): C

public inline fun <K, V, R, C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.mapTo(destination: C, transform: (kotlin.collections.Map.Entry<K, V>) -> R): C

public inline fun <K, V, R> kotlin.collections.Map<out K, V>.mapValues(transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map<K, R>

public inline fun <K, V, R, M : kotlin.collections.MutableMap<in K, in R>> kotlin.collections.Map<out K, V>.mapValuesTo(destination: M, transform: (kotlin.collections.Map.Entry<K, V>) -> R): M

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.max(): T?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.Array<out kotlin.Double>.max(): kotlin.Double?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.Array<out kotlin.Float>.max(): kotlin.Float?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ByteArray.max(): kotlin.Byte?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.CharArray.max(): kotlin.Char?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.max(): kotlin.Double?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.max(): kotlin.Float?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.IntArray.max(): kotlin.Int?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.LongArray.max(): kotlin.Long?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ShortArray.max(): kotlin.Short?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.max(): kotlin.UByte?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.max(): kotlin.UInt?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.max(): kotlin.ULong?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.max(): kotlin.UShort?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.max(): T?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.collections.Iterable<kotlin.Double>.max(): kotlin.Double?

@kotlin.Deprecated(message = "Use maxOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.collections.Iterable<kotlin.Float>.max(): kotlin.Float?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.maxBy(selector: (T) -> R): T?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.maxBy(selector: (kotlin.Boolean) -> R): kotlin.Boolean?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.maxBy(selector: (kotlin.Byte) -> R): kotlin.Byte?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.maxBy(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.maxBy(selector: (kotlin.Double) -> R): kotlin.Double?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.maxBy(selector: (kotlin.Float) -> R): kotlin.Float?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.maxBy(selector: (kotlin.Int) -> R): kotlin.Int?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.maxBy(selector: (kotlin.Long) -> R): kotlin.Long?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.maxBy(selector: (kotlin.Short) -> R): kotlin.Short?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.maxBy(selector: (kotlin.UByte) -> R): kotlin.UByte?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.maxBy(selector: (kotlin.UInt) -> R): kotlin.UInt?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.maxBy(selector: (kotlin.ULong) -> R): kotlin.ULong?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.maxBy(selector: (kotlin.UShort) -> R): kotlin.UShort?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxBy(selector: (T) -> R): T?

@kotlin.Deprecated(message = "Use maxByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxBy(selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.maxByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.maxByOrNull(selector: (kotlin.Boolean) -> R): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.maxByOrNull(selector: (kotlin.Byte) -> R): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.maxByOrNull(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.maxByOrNull(selector: (kotlin.Double) -> R): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.maxByOrNull(selector: (kotlin.Float) -> R): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.maxByOrNull(selector: (kotlin.Int) -> R): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.maxByOrNull(selector: (kotlin.Long) -> R): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.maxByOrNull(selector: (kotlin.Short) -> R): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.maxByOrNull(selector: (kotlin.UByte) -> R): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.maxByOrNull(selector: (kotlin.UInt) -> R): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.maxByOrNull(selector: (kotlin.ULong) -> R): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.maxByOrNull(selector: (kotlin.UShort) -> R): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxByOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.maxOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.maxOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.maxOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.maxOf(selector: (kotlin.Boolean) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.maxOf(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.maxOf(selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.maxOf(selector: (kotlin.Byte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.maxOf(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.maxOf(selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.maxOf(selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.maxOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.maxOf(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.maxOf(selector: (kotlin.Double) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.maxOf(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.maxOf(selector: (kotlin.Double) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.maxOf(selector: (kotlin.Float) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.maxOf(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.maxOf(selector: (kotlin.Float) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.maxOf(selector: (kotlin.Int) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.maxOf(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.maxOf(selector: (kotlin.Int) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.maxOf(selector: (kotlin.Long) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.maxOf(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.maxOf(selector: (kotlin.Long) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.maxOf(selector: (kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.maxOf(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.maxOf(selector: (kotlin.Short) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.maxOf(selector: (kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.maxOf(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.maxOf(selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.maxOf(selector: (kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.maxOf(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.maxOf(selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.maxOf(selector: (kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.maxOf(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.maxOf(selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.maxOf(selector: (kotlin.UShort) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.maxOf(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.maxOf(selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.maxOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.maxOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxOf(selector: (kotlin.collections.Map.Entry<K, V>) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxOf(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxOf(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.maxOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.maxOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.maxOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.maxOfOrNull(selector: (kotlin.Boolean) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.maxOfOrNull(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.maxOfOrNull(selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.maxOfOrNull(selector: (kotlin.Byte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.maxOfOrNull(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.maxOfOrNull(selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.maxOfOrNull(selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.maxOfOrNull(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.maxOfOrNull(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.maxOfOrNull(selector: (kotlin.Double) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.maxOfOrNull(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.maxOfOrNull(selector: (kotlin.Double) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.maxOfOrNull(selector: (kotlin.Float) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.maxOfOrNull(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.maxOfOrNull(selector: (kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.maxOfOrNull(selector: (kotlin.Int) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.maxOfOrNull(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.maxOfOrNull(selector: (kotlin.Int) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.maxOfOrNull(selector: (kotlin.Long) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.maxOfOrNull(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.maxOfOrNull(selector: (kotlin.Long) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.maxOfOrNull(selector: (kotlin.Short) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.maxOfOrNull(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.maxOfOrNull(selector: (kotlin.Short) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.maxOfOrNull(selector: (kotlin.UByte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.maxOfOrNull(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.maxOfOrNull(selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.maxOfOrNull(selector: (kotlin.UInt) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.maxOfOrNull(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.maxOfOrNull(selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.maxOfOrNull(selector: (kotlin.ULong) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.maxOfOrNull(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.maxOfOrNull(selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.maxOfOrNull(selector: (kotlin.UShort) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.maxOfOrNull(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.maxOfOrNull(selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.maxOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.maxOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Boolean) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Byte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Double) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Float) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Int) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Long) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UShort) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R> kotlin.collections.Map<out K, V>.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.collections.Map.Entry<K, V>) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Boolean) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Byte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Double) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Float) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Int) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Long) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Short) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UByte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UInt) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.ULong) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UShort) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R> kotlin.collections.Map<out K, V>.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.maxOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Array<out kotlin.Double>.maxOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Array<out kotlin.Float>.maxOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.maxOrNull(): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.maxOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.maxOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.maxOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.maxOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.maxOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.maxOrNull(): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.maxOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.maxOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.maxOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.maxOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.maxOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.collections.Iterable<kotlin.Double>.maxOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.collections.Iterable<kotlin.Float>.maxOrNull(): kotlin.Float?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T> kotlin.Array<out T>.maxWith(comparator: kotlin.Comparator<in T>): T?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.BooleanArray.maxWith(comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ByteArray.maxWith(comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.CharArray.maxWith(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.maxWith(comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.maxWith(comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.IntArray.maxWith(comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.LongArray.maxWith(comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ShortArray.maxWith(comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.maxWith(comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.maxWith(comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.maxWith(comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.maxWith(comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T> kotlin.collections.Iterable<T>.maxWith(comparator: kotlin.Comparator<in T>): T?

@kotlin.Deprecated(message = "Use maxWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.maxWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxWith(comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>.maxWithOrNull(comparator: kotlin.Comparator<in T>): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.collections.Iterable<T>.maxWithOrNull(comparator: kotlin.Comparator<in T>): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.min(): T?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.Array<out kotlin.Double>.min(): kotlin.Double?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.Array<out kotlin.Float>.min(): kotlin.Float?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ByteArray.min(): kotlin.Byte?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.CharArray.min(): kotlin.Char?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.min(): kotlin.Double?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.min(): kotlin.Float?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.IntArray.min(): kotlin.Int?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.LongArray.min(): kotlin.Long?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ShortArray.min(): kotlin.Short?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.min(): kotlin.UByte?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.min(): kotlin.UInt?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.min(): kotlin.ULong?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.min(): kotlin.UShort?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.min(): T?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.collections.Iterable<kotlin.Double>.min(): kotlin.Double?

@kotlin.Deprecated(message = "Use minOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minOrNull()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.collections.Iterable<kotlin.Float>.min(): kotlin.Float?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.minBy(selector: (T) -> R): T?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.minBy(selector: (kotlin.Boolean) -> R): kotlin.Boolean?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.minBy(selector: (kotlin.Byte) -> R): kotlin.Byte?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.minBy(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.minBy(selector: (kotlin.Double) -> R): kotlin.Double?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.minBy(selector: (kotlin.Float) -> R): kotlin.Float?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.minBy(selector: (kotlin.Int) -> R): kotlin.Int?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.minBy(selector: (kotlin.Long) -> R): kotlin.Long?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.minBy(selector: (kotlin.Short) -> R): kotlin.Short?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.minBy(selector: (kotlin.UByte) -> R): kotlin.UByte?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.minBy(selector: (kotlin.UInt) -> R): kotlin.UInt?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.minBy(selector: (kotlin.ULong) -> R): kotlin.ULong?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.minBy(selector: (kotlin.UShort) -> R): kotlin.UShort?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minBy(selector: (T) -> R): T?

@kotlin.Deprecated(message = "Use minByOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minByOrNull(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minBy(selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.minByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.minByOrNull(selector: (kotlin.Boolean) -> R): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.minByOrNull(selector: (kotlin.Byte) -> R): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.minByOrNull(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.minByOrNull(selector: (kotlin.Double) -> R): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.minByOrNull(selector: (kotlin.Float) -> R): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.minByOrNull(selector: (kotlin.Int) -> R): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.minByOrNull(selector: (kotlin.Long) -> R): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.minByOrNull(selector: (kotlin.Short) -> R): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.minByOrNull(selector: (kotlin.UByte) -> R): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.minByOrNull(selector: (kotlin.UInt) -> R): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.minByOrNull(selector: (kotlin.ULong) -> R): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.minByOrNull(selector: (kotlin.UShort) -> R): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minByOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.minOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.minOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.minOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.minOf(selector: (kotlin.Boolean) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.minOf(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.minOf(selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.minOf(selector: (kotlin.Byte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.minOf(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.minOf(selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.minOf(selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.minOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.minOf(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.minOf(selector: (kotlin.Double) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.minOf(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.minOf(selector: (kotlin.Double) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.minOf(selector: (kotlin.Float) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.minOf(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.minOf(selector: (kotlin.Float) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.minOf(selector: (kotlin.Int) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.minOf(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.minOf(selector: (kotlin.Int) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.minOf(selector: (kotlin.Long) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.minOf(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.minOf(selector: (kotlin.Long) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.minOf(selector: (kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.minOf(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.minOf(selector: (kotlin.Short) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.minOf(selector: (kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.minOf(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.minOf(selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.minOf(selector: (kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.minOf(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.minOf(selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.minOf(selector: (kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.minOf(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.minOf(selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.minOf(selector: (kotlin.UShort) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.minOf(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.minOf(selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.minOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.minOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minOf(selector: (kotlin.collections.Map.Entry<K, V>) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.minOf(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.minOf(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.minOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.minOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.minOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.minOfOrNull(selector: (kotlin.Boolean) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.minOfOrNull(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.minOfOrNull(selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.minOfOrNull(selector: (kotlin.Byte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.minOfOrNull(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.minOfOrNull(selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.minOfOrNull(selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.minOfOrNull(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.minOfOrNull(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.minOfOrNull(selector: (kotlin.Double) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.minOfOrNull(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.minOfOrNull(selector: (kotlin.Double) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.minOfOrNull(selector: (kotlin.Float) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.minOfOrNull(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.minOfOrNull(selector: (kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.minOfOrNull(selector: (kotlin.Int) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.minOfOrNull(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.minOfOrNull(selector: (kotlin.Int) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.minOfOrNull(selector: (kotlin.Long) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.minOfOrNull(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.minOfOrNull(selector: (kotlin.Long) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.minOfOrNull(selector: (kotlin.Short) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.minOfOrNull(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.minOfOrNull(selector: (kotlin.Short) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UByteArray.minOfOrNull(selector: (kotlin.UByte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.minOfOrNull(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.minOfOrNull(selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UIntArray.minOfOrNull(selector: (kotlin.UInt) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.minOfOrNull(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.minOfOrNull(selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.ULongArray.minOfOrNull(selector: (kotlin.ULong) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.minOfOrNull(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.minOfOrNull(selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.UShortArray.minOfOrNull(selector: (kotlin.UShort) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.minOfOrNull(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.minOfOrNull(selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.minOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.minOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.minOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.minOfOrNull(selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.minOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Boolean) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Byte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Double) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Float) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Int) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Long) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Short) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UByte) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UInt) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.ULong) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.UShort) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.minOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R> kotlin.collections.Map<out K, V>.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.collections.Map.Entry<K, V>) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.Array<out T>.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Boolean) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Byte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Double) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Float) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Int) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Long) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Short) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UByte) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UInt) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.ULong) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.UShort) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.collections.Iterable<T>.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <K, V, R> kotlin.collections.Map<out K, V>.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.minOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Array<out kotlin.Double>.minOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.Array<out kotlin.Float>.minOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.minOrNull(): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.minOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.minOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.minOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.minOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.minOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.minOrNull(): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.minOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.minOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.minOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.minOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.minOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.collections.Iterable<kotlin.Double>.minOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.collections.Iterable<kotlin.Float>.minOrNull(): kotlin.Float?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T> kotlin.Array<out T>.minWith(comparator: kotlin.Comparator<in T>): T?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.BooleanArray.minWith(comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ByteArray.minWith(comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.CharArray.minWith(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.DoubleArray.minWith(comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.FloatArray.minWith(comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.IntArray.minWith(comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.LongArray.minWith(comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun kotlin.ShortArray.minWith(comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.minWith(comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.minWith(comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.minWith(comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.minWith(comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <T> kotlin.collections.Iterable<T>.minWith(comparator: kotlin.Comparator<in T>): T?

@kotlin.Deprecated(message = "Use minWithOrNull instead.", replaceWith = kotlin.ReplaceWith(expression = "this.minWithOrNull(comparator)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.4")
public fun <K, V> kotlin.collections.Map<out K, V>.minWith(comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>.minWithOrNull(comparator: kotlin.Comparator<in T>): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.minWithOrNull(comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.collections.Iterable<T>.minWithOrNull(comparator: kotlin.Comparator<in T>): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<out K, V>.minWithOrNull(comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?

public operator fun <T> kotlin.collections.Iterable<T>.minus(element: T): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.minus(elements: kotlin.Array<out T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.minus(elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.minus(elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>

@kotlin.SinceKotlin(version = "1.1")
public operator fun <K, V> kotlin.collections.Map<out K, V>.minus(key: K): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.1")
public operator fun <K, V> kotlin.collections.Map<out K, V>.minus(keys: kotlin.Array<out K>): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.1")
public operator fun <K, V> kotlin.collections.Map<out K, V>.minus(keys: kotlin.collections.Iterable<K>): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.1")
public operator fun <K, V> kotlin.collections.Map<out K, V>.minus(keys: kotlin.sequences.Sequence<K>): kotlin.collections.Map<K, V>

public operator fun <T> kotlin.collections.Set<T>.minus(element: T): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.minus(elements: kotlin.Array<out T>): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.minus(elements: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.minus(elements: kotlin.sequences.Sequence<T>): kotlin.collections.Set<T>

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.minusAssign(element: T): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.minusAssign(elements: kotlin.Array<T>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.minusAssign(elements: kotlin.collections.Iterable<T>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.minusAssign(elements: kotlin.sequences.Sequence<T>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.minusAssign(key: K): kotlin.Unit

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.minusAssign(keys: kotlin.Array<out K>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.minusAssign(keys: kotlin.collections.Iterable<K>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.minusAssign(keys: kotlin.sequences.Sequence<K>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.minusElement(element: T): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Set<T>.minusElement(element: T): kotlin.collections.Set<T>

public fun <T> kotlin.Array<out T>.none(): kotlin.Boolean

public inline fun <T> kotlin.Array<out T>.none(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.BooleanArray.none(): kotlin.Boolean

public inline fun kotlin.BooleanArray.none(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ByteArray.none(): kotlin.Boolean

public inline fun kotlin.ByteArray.none(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.CharArray.none(): kotlin.Boolean

public inline fun kotlin.CharArray.none(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.DoubleArray.none(): kotlin.Boolean

public inline fun kotlin.DoubleArray.none(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.FloatArray.none(): kotlin.Boolean

public inline fun kotlin.FloatArray.none(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.IntArray.none(): kotlin.Boolean

public inline fun kotlin.IntArray.none(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.LongArray.none(): kotlin.Boolean

public inline fun kotlin.LongArray.none(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ShortArray.none(): kotlin.Boolean

public inline fun kotlin.ShortArray.none(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.none(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.none(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.none(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.none(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.none(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.none(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.none(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.none(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.collections.Iterable<T>.none(): kotlin.Boolean

public inline fun <T> kotlin.collections.Iterable<T>.none(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <K, V> kotlin.collections.Map<out K, V>.none(): kotlin.Boolean

public inline fun <K, V> kotlin.collections.Map<out K, V>.none(predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, C : kotlin.collections.Iterable<T>> C.onEach(action: (T) -> kotlin.Unit): C

@kotlin.SinceKotlin(version = "1.1")
public inline fun <K, V, M : kotlin.collections.Map<out K, V>> M.onEach(action: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.onEach(action: (T) -> kotlin.Unit): kotlin.Array<out T>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.onEach(action: (kotlin.Boolean) -> kotlin.Unit): kotlin.BooleanArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.onEach(action: (kotlin.Byte) -> kotlin.Unit): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.onEach(action: (kotlin.Char) -> kotlin.Unit): kotlin.CharArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.onEach(action: (kotlin.Double) -> kotlin.Unit): kotlin.DoubleArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.onEach(action: (kotlin.Float) -> kotlin.Unit): kotlin.FloatArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.onEach(action: (kotlin.Int) -> kotlin.Unit): kotlin.IntArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.onEach(action: (kotlin.Long) -> kotlin.Unit): kotlin.LongArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.onEach(action: (kotlin.Short) -> kotlin.Unit): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.onEach(action: (kotlin.UByte) -> kotlin.Unit): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.onEach(action: (kotlin.UInt) -> kotlin.Unit): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.onEach(action: (kotlin.ULong) -> kotlin.Unit): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.onEach(action: (kotlin.UShort) -> kotlin.Unit): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, C : kotlin.collections.Iterable<T>> C.onEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): C

@kotlin.SinceKotlin(version = "1.4")
public inline fun <K, V, M : kotlin.collections.Map<out K, V>> M.onEachIndexed(action: (index: kotlin.Int, kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): M

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.onEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Array<out T>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Unit): kotlin.BooleanArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Byte) -> kotlin.Unit): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.CharArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Double) -> kotlin.Unit): kotlin.DoubleArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Float) -> kotlin.Unit): kotlin.FloatArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Int) -> kotlin.Unit): kotlin.IntArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Long) -> kotlin.Unit): kotlin.LongArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.onEachIndexed(action: (index: kotlin.Int, kotlin.Short) -> kotlin.Unit): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.onEachIndexed(action: (index: kotlin.Int, kotlin.UByte) -> kotlin.Unit): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.onEachIndexed(action: (index: kotlin.Int, kotlin.UInt) -> kotlin.Unit): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.onEachIndexed(action: (index: kotlin.Int, kotlin.ULong) -> kotlin.Unit): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.onEachIndexed(action: (index: kotlin.Int, kotlin.UShort) -> kotlin.Unit): kotlin.UShortArray

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>?.orEmpty(): kotlin.Array<out T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>?.orEmpty(): kotlin.collections.Collection<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.List<T>?.orEmpty(): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map<K, V>?.orEmpty(): kotlin.collections.Map<K, V>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Set<T>?.orEmpty(): kotlin.collections.Set<T>

public inline fun <T> kotlin.Array<out T>.partition(predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>

public inline fun kotlin.BooleanArray.partition(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Boolean>, kotlin.collections.List<kotlin.Boolean>>

public inline fun kotlin.ByteArray.partition(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Byte>, kotlin.collections.List<kotlin.Byte>>

public inline fun kotlin.CharArray.partition(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Char>, kotlin.collections.List<kotlin.Char>>

public inline fun kotlin.DoubleArray.partition(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Double>, kotlin.collections.List<kotlin.Double>>

public inline fun kotlin.FloatArray.partition(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Float>, kotlin.collections.List<kotlin.Float>>

public inline fun kotlin.IntArray.partition(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Int>, kotlin.collections.List<kotlin.Int>>

public inline fun kotlin.LongArray.partition(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Long>, kotlin.collections.List<kotlin.Long>>

public inline fun kotlin.ShortArray.partition(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Short>, kotlin.collections.List<kotlin.Short>>

public inline fun <T> kotlin.collections.Iterable<T>.partition(predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>

public inline operator fun <T> kotlin.Array<out T>.plus(element: T): kotlin.Array<T>

public inline operator fun <T> kotlin.Array<out T>.plus(elements: kotlin.Array<out T>): kotlin.Array<T>

public operator fun <T> kotlin.Array<out T>.plus(elements: kotlin.collections.Collection<T>): kotlin.Array<T>

public inline operator fun kotlin.BooleanArray.plus(element: kotlin.Boolean): kotlin.BooleanArray

public inline operator fun kotlin.BooleanArray.plus(elements: kotlin.BooleanArray): kotlin.BooleanArray

public operator fun kotlin.BooleanArray.plus(elements: kotlin.collections.Collection<kotlin.Boolean>): kotlin.BooleanArray

public inline operator fun kotlin.ByteArray.plus(element: kotlin.Byte): kotlin.ByteArray

public inline operator fun kotlin.ByteArray.plus(elements: kotlin.ByteArray): kotlin.ByteArray

public operator fun kotlin.ByteArray.plus(elements: kotlin.collections.Collection<kotlin.Byte>): kotlin.ByteArray

public inline operator fun kotlin.CharArray.plus(element: kotlin.Char): kotlin.CharArray

public inline operator fun kotlin.CharArray.plus(elements: kotlin.CharArray): kotlin.CharArray

public operator fun kotlin.CharArray.plus(elements: kotlin.collections.Collection<kotlin.Char>): kotlin.CharArray

public inline operator fun kotlin.DoubleArray.plus(element: kotlin.Double): kotlin.DoubleArray

public inline operator fun kotlin.DoubleArray.plus(elements: kotlin.DoubleArray): kotlin.DoubleArray

public operator fun kotlin.DoubleArray.plus(elements: kotlin.collections.Collection<kotlin.Double>): kotlin.DoubleArray

public inline operator fun kotlin.FloatArray.plus(element: kotlin.Float): kotlin.FloatArray

public inline operator fun kotlin.FloatArray.plus(elements: kotlin.FloatArray): kotlin.FloatArray

public operator fun kotlin.FloatArray.plus(elements: kotlin.collections.Collection<kotlin.Float>): kotlin.FloatArray

public inline operator fun kotlin.IntArray.plus(element: kotlin.Int): kotlin.IntArray

public inline operator fun kotlin.IntArray.plus(elements: kotlin.IntArray): kotlin.IntArray

public operator fun kotlin.IntArray.plus(elements: kotlin.collections.Collection<kotlin.Int>): kotlin.IntArray

public inline operator fun kotlin.LongArray.plus(element: kotlin.Long): kotlin.LongArray

public inline operator fun kotlin.LongArray.plus(elements: kotlin.LongArray): kotlin.LongArray

public operator fun kotlin.LongArray.plus(elements: kotlin.collections.Collection<kotlin.Long>): kotlin.LongArray

public inline operator fun kotlin.ShortArray.plus(element: kotlin.Short): kotlin.ShortArray

public inline operator fun kotlin.ShortArray.plus(elements: kotlin.ShortArray): kotlin.ShortArray

public operator fun kotlin.ShortArray.plus(elements: kotlin.collections.Collection<kotlin.Short>): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.plus(element: kotlin.UByte): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UByteArray.plus(elements: kotlin.UByteArray): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public operator fun kotlin.UByteArray.plus(elements: kotlin.collections.Collection<kotlin.UByte>): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.plus(element: kotlin.UInt): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UIntArray.plus(elements: kotlin.UIntArray): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public operator fun kotlin.UIntArray.plus(elements: kotlin.collections.Collection<kotlin.UInt>): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.plus(element: kotlin.ULong): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.ULongArray.plus(elements: kotlin.ULongArray): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public operator fun kotlin.ULongArray.plus(elements: kotlin.collections.Collection<kotlin.ULong>): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.plus(element: kotlin.UShort): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline operator fun kotlin.UShortArray.plus(elements: kotlin.UShortArray): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public operator fun kotlin.UShortArray.plus(elements: kotlin.collections.Collection<kotlin.UShort>): kotlin.UShortArray

public operator fun <T> kotlin.collections.Collection<T>.plus(element: T): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Collection<T>.plus(elements: kotlin.Array<out T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Collection<T>.plus(elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Collection<T>.plus(elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.plus(element: T): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.plus(elements: kotlin.Array<out T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.plus(elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>

public operator fun <T> kotlin.collections.Iterable<T>.plus(elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>

public operator fun <K, V> kotlin.collections.Map<out K, V>.plus(pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>

public operator fun <K, V> kotlin.collections.Map<out K, V>.plus(pair: kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public operator fun <K, V> kotlin.collections.Map<out K, V>.plus(pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>

public operator fun <K, V> kotlin.collections.Map<out K, V>.plus(map: kotlin.collections.Map<out K, V>): kotlin.collections.Map<K, V>

public operator fun <K, V> kotlin.collections.Map<out K, V>.plus(pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>

public operator fun <T> kotlin.collections.Set<T>.plus(element: T): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.plus(elements: kotlin.Array<out T>): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.plus(elements: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public operator fun <T> kotlin.collections.Set<T>.plus(elements: kotlin.sequences.Sequence<T>): kotlin.collections.Set<T>

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.plusAssign(element: T): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.plusAssign(elements: kotlin.Array<T>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.plusAssign(elements: kotlin.collections.Iterable<T>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <T> kotlin.collections.MutableCollection<in T>.plusAssign(elements: kotlin.sequences.Sequence<T>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<in K, in V>.plusAssign(pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<in K, in V>.plusAssign(pair: kotlin.Pair<K, V>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<in K, in V>.plusAssign(pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<in K, in V>.plusAssign(map: kotlin.collections.Map<K, V>): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<in K, in V>.plusAssign(pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.Unit

public inline fun <T> kotlin.Array<out T>.plusElement(element: T): kotlin.Array<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.plusElement(element: T): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.plusElement(element: T): kotlin.collections.List<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Set<T>.plusElement(element: T): kotlin.collections.Set<T>

public fun <K, V> kotlin.collections.MutableMap<in K, in V>.putAll(pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.Unit

public fun <K, V> kotlin.collections.MutableMap<in K, in V>.putAll(pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.Unit

public fun <K, V> kotlin.collections.MutableMap<in K, in V>.putAll(pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.random(): T

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.Array<out T>.random(random: kotlin.random.Random): T

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.random(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.BooleanArray.random(random: kotlin.random.Random): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.random(): kotlin.Byte

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ByteArray.random(random: kotlin.random.Random): kotlin.Byte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.random(): kotlin.Char

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.CharArray.random(random: kotlin.random.Random): kotlin.Char

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.random(): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.DoubleArray.random(random: kotlin.random.Random): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.random(): kotlin.Float

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.FloatArray.random(random: kotlin.random.Random): kotlin.Float

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.random(): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.IntArray.random(random: kotlin.random.Random): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.random(): kotlin.Long

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.LongArray.random(random: kotlin.random.Random): kotlin.Long

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.random(): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.ShortArray.random(random: kotlin.random.Random): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.random(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.random(random: kotlin.random.Random): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.random(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.random(random: kotlin.random.Random): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.random(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.random(random: kotlin.random.Random): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.random(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.random(random: kotlin.random.Random): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.random(): T

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.collections.Collection<T>.random(random: kotlin.random.Random): T

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.randomOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.Array<out T>.randomOrNull(random: kotlin.random.Random): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.randomOrNull(): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.BooleanArray.randomOrNull(random: kotlin.random.Random): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.randomOrNull(): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.ByteArray.randomOrNull(random: kotlin.random.Random): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.randomOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.CharArray.randomOrNull(random: kotlin.random.Random): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.randomOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.DoubleArray.randomOrNull(random: kotlin.random.Random): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.randomOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.FloatArray.randomOrNull(random: kotlin.random.Random): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.randomOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.IntArray.randomOrNull(random: kotlin.random.Random): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.randomOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.LongArray.randomOrNull(random: kotlin.random.Random): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.randomOrNull(): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.ShortArray.randomOrNull(random: kotlin.random.Random): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.randomOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.randomOrNull(random: kotlin.random.Random): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.randomOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.randomOrNull(random: kotlin.random.Random): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.randomOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.randomOrNull(random: kotlin.random.Random): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.randomOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.randomOrNull(random: kotlin.random.Random): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.randomOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.collections.Collection<T>.randomOrNull(random: kotlin.random.Random): T?

public inline fun <S, T : S> kotlin.Array<out T>.reduce(operation: (acc: S, T) -> S): S

public inline fun kotlin.BooleanArray.reduce(operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ByteArray.reduce(operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte

public inline fun kotlin.CharArray.reduce(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.DoubleArray.reduce(operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double

public inline fun kotlin.FloatArray.reduce(operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float

public inline fun kotlin.IntArray.reduce(operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int

public inline fun kotlin.LongArray.reduce(operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long

public inline fun kotlin.ShortArray.reduce(operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduce(operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduce(operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduce(operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduce(operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort

@kotlin.SinceKotlin(version = "1.1")
public inline fun <S, T : S, K> kotlin.collections.Grouping<T, K>.reduce(operation: (key: K, accumulator: S, element: T) -> S): kotlin.collections.Map<K, S>

public inline fun <S, T : S> kotlin.collections.Iterable<T>.reduce(operation: (acc: S, T) -> S): S

public inline fun <S, T : S> kotlin.Array<out T>.reduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): S

public inline fun kotlin.BooleanArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ByteArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte

public inline fun kotlin.CharArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.DoubleArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double

public inline fun kotlin.FloatArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float

public inline fun kotlin.IntArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int

public inline fun kotlin.LongArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long

public inline fun kotlin.ShortArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort

public inline fun <S, T : S> kotlin.collections.Iterable<T>.reduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): S

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.Array<out T>.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: S, T) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.BooleanArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.ByteArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.DoubleArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.FloatArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.IntArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.LongArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.ShortArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.collections.Iterable<T>.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: S, T) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.Array<out T>.reduceOrNull(operation: (acc: S, T) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.BooleanArray.reduceOrNull(operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.ByteArray.reduceOrNull(operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.CharArray.reduceOrNull(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.DoubleArray.reduceOrNull(operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.FloatArray.reduceOrNull(operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.IntArray.reduceOrNull(operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.LongArray.reduceOrNull(operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.ShortArray.reduceOrNull(operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceOrNull(operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceOrNull(operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceOrNull(operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceOrNull(operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.collections.Iterable<T>.reduceOrNull(operation: (acc: S, T) -> S): S?

public inline fun <S, T : S> kotlin.Array<out T>.reduceRight(operation: (T, acc: S) -> S): S

public inline fun kotlin.BooleanArray.reduceRight(operation: (kotlin.Boolean, acc: kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ByteArray.reduceRight(operation: (kotlin.Byte, acc: kotlin.Byte) -> kotlin.Byte): kotlin.Byte

public inline fun kotlin.CharArray.reduceRight(operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.DoubleArray.reduceRight(operation: (kotlin.Double, acc: kotlin.Double) -> kotlin.Double): kotlin.Double

public inline fun kotlin.FloatArray.reduceRight(operation: (kotlin.Float, acc: kotlin.Float) -> kotlin.Float): kotlin.Float

public inline fun kotlin.IntArray.reduceRight(operation: (kotlin.Int, acc: kotlin.Int) -> kotlin.Int): kotlin.Int

public inline fun kotlin.LongArray.reduceRight(operation: (kotlin.Long, acc: kotlin.Long) -> kotlin.Long): kotlin.Long

public inline fun kotlin.ShortArray.reduceRight(operation: (kotlin.Short, acc: kotlin.Short) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceRight(operation: (kotlin.UByte, acc: kotlin.UByte) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceRight(operation: (kotlin.UInt, acc: kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceRight(operation: (kotlin.ULong, acc: kotlin.ULong) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceRight(operation: (kotlin.UShort, acc: kotlin.UShort) -> kotlin.UShort): kotlin.UShort

public inline fun <S, T : S> kotlin.collections.List<T>.reduceRight(operation: (T, acc: S) -> S): S

public inline fun <S, T : S> kotlin.Array<out T>.reduceRightIndexed(operation: (index: kotlin.Int, T, acc: S) -> S): S

public inline fun kotlin.BooleanArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Boolean, acc: kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public inline fun kotlin.ByteArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Byte, acc: kotlin.Byte) -> kotlin.Byte): kotlin.Byte

public inline fun kotlin.CharArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.DoubleArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Double, acc: kotlin.Double) -> kotlin.Double): kotlin.Double

public inline fun kotlin.FloatArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Float, acc: kotlin.Float) -> kotlin.Float): kotlin.Float

public inline fun kotlin.IntArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Int, acc: kotlin.Int) -> kotlin.Int): kotlin.Int

public inline fun kotlin.LongArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Long, acc: kotlin.Long) -> kotlin.Long): kotlin.Long

public inline fun kotlin.ShortArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Short, acc: kotlin.Short) -> kotlin.Short): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.UByte, acc: kotlin.UByte) -> kotlin.UByte): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.UInt, acc: kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.ULong, acc: kotlin.ULong) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.UShort, acc: kotlin.UShort) -> kotlin.UShort): kotlin.UShort

public inline fun <S, T : S> kotlin.collections.List<T>.reduceRightIndexed(operation: (index: kotlin.Int, T, acc: S) -> S): S

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.Array<out T>.reduceRightIndexedOrNull(operation: (index: kotlin.Int, T, acc: S) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.BooleanArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Boolean, acc: kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.ByteArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Byte, acc: kotlin.Byte) -> kotlin.Byte): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.DoubleArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Double, acc: kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.FloatArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Float, acc: kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.IntArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Int, acc: kotlin.Int) -> kotlin.Int): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.LongArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Long, acc: kotlin.Long) -> kotlin.Long): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.ShortArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Short, acc: kotlin.Short) -> kotlin.Short): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.UByte, acc: kotlin.UByte) -> kotlin.UByte): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.UInt, acc: kotlin.UInt) -> kotlin.UInt): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.ULong, acc: kotlin.ULong) -> kotlin.ULong): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.UShort, acc: kotlin.UShort) -> kotlin.UShort): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.collections.List<T>.reduceRightIndexedOrNull(operation: (index: kotlin.Int, T, acc: S) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.Array<out T>.reduceRightOrNull(operation: (T, acc: S) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.BooleanArray.reduceRightOrNull(operation: (kotlin.Boolean, acc: kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.ByteArray.reduceRightOrNull(operation: (kotlin.Byte, acc: kotlin.Byte) -> kotlin.Byte): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.CharArray.reduceRightOrNull(operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.DoubleArray.reduceRightOrNull(operation: (kotlin.Double, acc: kotlin.Double) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.FloatArray.reduceRightOrNull(operation: (kotlin.Float, acc: kotlin.Float) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.IntArray.reduceRightOrNull(operation: (kotlin.Int, acc: kotlin.Int) -> kotlin.Int): kotlin.Int?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.LongArray.reduceRightOrNull(operation: (kotlin.Long, acc: kotlin.Long) -> kotlin.Long): kotlin.Long?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.ShortArray.reduceRightOrNull(operation: (kotlin.Short, acc: kotlin.Short) -> kotlin.Short): kotlin.Short?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reduceRightOrNull(operation: (kotlin.UByte, acc: kotlin.UByte) -> kotlin.UByte): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reduceRightOrNull(operation: (kotlin.UInt, acc: kotlin.UInt) -> kotlin.UInt): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reduceRightOrNull(operation: (kotlin.ULong, acc: kotlin.ULong) -> kotlin.ULong): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reduceRightOrNull(operation: (kotlin.UShort, acc: kotlin.UShort) -> kotlin.UShort): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.collections.List<T>.reduceRightOrNull(operation: (T, acc: S) -> S): S?

@kotlin.SinceKotlin(version = "1.1")
public inline fun <S, T : S, K, M : kotlin.collections.MutableMap<in K, S>> kotlin.collections.Grouping<T, K>.reduceTo(destination: M, operation: (key: K, accumulator: S, element: T) -> S): M

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.MutableCollection<out T>.remove(element: T): kotlin.Boolean

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use removeAt(index) instead.", replaceWith = kotlin.ReplaceWith(expression = "removeAt(index)", imports = {}))
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.MutableList<T>.remove(index: kotlin.Int): T

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
K, V> kotlin.collections.MutableMap<out K, V>.remove(key: K): V?

public fun <T> kotlin.collections.MutableCollection<in T>.removeAll(elements: kotlin.Array<out T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.removeAll(elements: kotlin.collections.Iterable<T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.removeAll(elements: kotlin.sequences.Sequence<T>): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.MutableCollection<out T>.removeAll(elements: kotlin.collections.Collection<T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableIterable<T>.removeAll(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.collections.MutableList<T>.removeAll(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.collections.MutableList<T>.removeFirst(): T

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.collections.MutableList<T>.removeFirstOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.collections.MutableList<T>.removeLast(): T

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T> kotlin.collections.MutableList<T>.removeLastOrNull(): T?

public fun <T : kotlin.Any> kotlin.Array<T?>.requireNoNulls(): kotlin.Array<T>

public fun <T : kotlin.Any> kotlin.collections.Iterable<T?>.requireNoNulls(): kotlin.collections.Iterable<T>

public fun <T : kotlin.Any> kotlin.collections.List<T?>.requireNoNulls(): kotlin.collections.List<T>

public fun <T> kotlin.collections.MutableCollection<in T>.retainAll(elements: kotlin.Array<out T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.retainAll(elements: kotlin.collections.Iterable<T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableCollection<in T>.retainAll(elements: kotlin.sequences.Sequence<T>): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes
T> kotlin.collections.MutableCollection<out T>.retainAll(elements: kotlin.collections.Collection<T>): kotlin.Boolean

public fun <T> kotlin.collections.MutableIterable<T>.retainAll(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.collections.MutableList<T>.retainAll(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.Array<T>.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<T>.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.BooleanArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.ByteArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.CharArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.DoubleArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.FloatArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.IntArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.LongArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.ShortArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reverse(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reverse(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun <T> kotlin.collections.MutableList<T>.reverse(): kotlin.Unit

public fun <T> kotlin.Array<out T>.reversed(): kotlin.collections.List<T>

public fun kotlin.BooleanArray.reversed(): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.reversed(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.reversed(): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.reversed(): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.reversed(): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.reversed(): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.reversed(): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.reversed(): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.reversed(): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.reversed(): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.reversed(): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.reversed(): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.Iterable<T>.reversed(): kotlin.collections.List<T>

public fun <T> kotlin.Array<T>.reversedArray(): kotlin.Array<T>

public fun kotlin.BooleanArray.reversedArray(): kotlin.BooleanArray

public fun kotlin.ByteArray.reversedArray(): kotlin.ByteArray

public fun kotlin.CharArray.reversedArray(): kotlin.CharArray

public fun kotlin.DoubleArray.reversedArray(): kotlin.DoubleArray

public fun kotlin.FloatArray.reversedArray(): kotlin.FloatArray

public fun kotlin.IntArray.reversedArray(): kotlin.IntArray

public fun kotlin.LongArray.reversedArray(): kotlin.LongArray

public fun kotlin.ShortArray.reversedArray(): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.reversedArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.reversedArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.reversedArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.reversedArray(): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R> kotlin.Array<out T>.runningFold(initial: R, operation: (acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.runningFold(initial: R, operation: (acc: R, kotlin.Boolean) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.runningFold(initial: R, operation: (acc: R, kotlin.Byte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.runningFold(initial: R, operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.runningFold(initial: R, operation: (acc: R, kotlin.Double) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.runningFold(initial: R, operation: (acc: R, kotlin.Float) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.runningFold(initial: R, operation: (acc: R, kotlin.Int) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.runningFold(initial: R, operation: (acc: R, kotlin.Long) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.runningFold(initial: R, operation: (acc: R, kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.runningFold(initial: R, operation: (acc: R, kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.runningFold(initial: R, operation: (acc: R, kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.runningFold(initial: R, operation: (acc: R, kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.runningFold(initial: R, operation: (acc: R, kotlin.UShort) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R> kotlin.collections.Iterable<T>.runningFold(initial: R, operation: (acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R> kotlin.Array<out T>.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Boolean) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Byte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Double) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Float) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Int) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Long) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UShort) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R> kotlin.collections.Iterable<T>.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.Array<out T>.runningReduce(operation: (acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.runningReduce(operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.runningReduce(operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.collections.List<kotlin.Byte>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.runningReduce(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.runningReduce(operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.collections.List<kotlin.Double>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.runningReduce(operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.collections.List<kotlin.Float>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.runningReduce(operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.collections.List<kotlin.Int>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.runningReduce(operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.collections.List<kotlin.Long>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.runningReduce(operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.runningReduce(operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.runningReduce(operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.runningReduce(operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.runningReduce(operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.collections.List<kotlin.UShort>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <S, T : S> kotlin.collections.Iterable<T>.runningReduce(operation: (acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.Array<out T>.runningReduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.collections.List<kotlin.Byte>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.collections.List<kotlin.Double>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.collections.List<kotlin.Float>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.collections.List<kotlin.Int>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.collections.List<kotlin.Long>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.collections.List<kotlin.UShort>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.collections.Iterable<T>.runningReduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <T, R> kotlin.Array<out T>.scan(initial: R, operation: (acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.scan(initial: R, operation: (acc: R, kotlin.Boolean) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.scan(initial: R, operation: (acc: R, kotlin.Byte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.scan(initial: R, operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.scan(initial: R, operation: (acc: R, kotlin.Double) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.scan(initial: R, operation: (acc: R, kotlin.Float) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.scan(initial: R, operation: (acc: R, kotlin.Int) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.scan(initial: R, operation: (acc: R, kotlin.Long) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.scan(initial: R, operation: (acc: R, kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.scan(initial: R, operation: (acc: R, kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.scan(initial: R, operation: (acc: R, kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.scan(initial: R, operation: (acc: R, kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.scan(initial: R, operation: (acc: R, kotlin.UShort) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <T, R> kotlin.collections.Iterable<T>.scan(initial: R, operation: (acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <T, R> kotlin.Array<out T>.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.BooleanArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Boolean) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ByteArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Byte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.DoubleArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Double) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.FloatArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Float) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.IntArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Int) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.LongArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Long) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ShortArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Short) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UByteArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UByte) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UIntArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UInt) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.ULongArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.ULong) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.UShortArray.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.UShort) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <T, R> kotlin.collections.Iterable<T>.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.collections.List<R>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public inline fun <S, T : S> kotlin.Array<out T>.scanReduce(operation: (acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.scanReduce(operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.scanReduce(operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.collections.List<kotlin.Byte>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.scanReduce(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.scanReduce(operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.collections.List<kotlin.Double>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.scanReduce(operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.collections.List<kotlin.Float>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.scanReduce(operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.collections.List<kotlin.Int>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.scanReduce(operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.collections.List<kotlin.Long>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.scanReduce(operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.collections.List<kotlin.Short>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.scanReduce(operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.collections.List<kotlin.UByte>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.scanReduce(operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.collections.List<kotlin.UInt>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.scanReduce(operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.collections.List<kotlin.ULong>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.scanReduce(operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.collections.List<kotlin.UShort>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public inline fun <S, T : S> kotlin.collections.Iterable<T>.scanReduce(operation: (acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public inline fun <S, T : S> kotlin.Array<out T>.scanReduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.collections.List<kotlin.Byte>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.collections.List<kotlin.Double>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.collections.List<kotlin.Float>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.collections.List<kotlin.Int>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.collections.List<kotlin.Long>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.collections.List<kotlin.Short>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.collections.List<kotlin.UByte>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.collections.List<kotlin.UInt>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.collections.List<kotlin.ULong>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.scanReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.collections.List<kotlin.UShort>

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {}))
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public inline fun <S, T : S> kotlin.collections.Iterable<T>.scanReduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.collections.List<S>

@kotlin.internal.InlineOnly
public inline operator fun <K, V> kotlin.collections.MutableMap<K, V>.set(key: K, value: V): kotlin.Unit

@kotlin.internal.InlineOnly
public inline operator fun <V> kotlin.collections.MutableMap<in kotlin.String, in V>.setValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>, value: V): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<T>.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<T>.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.BooleanArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.MutableList<T>.shuffle(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.collections.MutableList<T>.shuffle(random: kotlin.random.Random): kotlin.Unit

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.Iterable<T>.shuffled(): kotlin.collections.List<T>

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.collections.Iterable<T>.shuffled(random: kotlin.random.Random): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.single(): T

public inline fun <T> kotlin.Array<out T>.single(predicate: (T) -> kotlin.Boolean): T

public fun kotlin.BooleanArray.single(): kotlin.Boolean

public inline fun kotlin.BooleanArray.single(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.ByteArray.single(): kotlin.Byte

public inline fun kotlin.ByteArray.single(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte

public fun kotlin.CharArray.single(): kotlin.Char

public inline fun kotlin.CharArray.single(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

public fun kotlin.DoubleArray.single(): kotlin.Double

public inline fun kotlin.DoubleArray.single(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double

public fun kotlin.FloatArray.single(): kotlin.Float

public inline fun kotlin.FloatArray.single(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float

public fun kotlin.IntArray.single(): kotlin.Int

public inline fun kotlin.IntArray.single(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int

public fun kotlin.LongArray.single(): kotlin.Long

public inline fun kotlin.LongArray.single(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long

public fun kotlin.ShortArray.single(): kotlin.Short

public inline fun kotlin.ShortArray.single(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.single(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.single(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.single(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.single(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.single(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.single(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.single(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.single(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort

public fun <T> kotlin.collections.Iterable<T>.single(): T

public inline fun <T> kotlin.collections.Iterable<T>.single(predicate: (T) -> kotlin.Boolean): T

public fun <T> kotlin.collections.List<T>.single(): T

public fun <T> kotlin.Array<out T>.singleOrNull(): T?

public inline fun <T> kotlin.Array<out T>.singleOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun kotlin.BooleanArray.singleOrNull(): kotlin.Boolean?

public inline fun kotlin.BooleanArray.singleOrNull(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?

public fun kotlin.ByteArray.singleOrNull(): kotlin.Byte?

public inline fun kotlin.ByteArray.singleOrNull(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?

public fun kotlin.CharArray.singleOrNull(): kotlin.Char?

public inline fun kotlin.CharArray.singleOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.DoubleArray.singleOrNull(): kotlin.Double?

public inline fun kotlin.DoubleArray.singleOrNull(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?

public fun kotlin.FloatArray.singleOrNull(): kotlin.Float?

public inline fun kotlin.FloatArray.singleOrNull(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?

public fun kotlin.IntArray.singleOrNull(): kotlin.Int?

public inline fun kotlin.IntArray.singleOrNull(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?

public fun kotlin.LongArray.singleOrNull(): kotlin.Long?

public inline fun kotlin.LongArray.singleOrNull(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?

public fun kotlin.ShortArray.singleOrNull(): kotlin.Short?

public inline fun kotlin.ShortArray.singleOrNull(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.singleOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.singleOrNull(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.singleOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.singleOrNull(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.singleOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.singleOrNull(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.singleOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.singleOrNull(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?

public fun <T> kotlin.collections.Iterable<T>.singleOrNull(): T?

public inline fun <T> kotlin.collections.Iterable<T>.singleOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun <T> kotlin.collections.List<T>.singleOrNull(): T?

public fun <T> kotlin.Array<out T>.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<T>

public fun kotlin.BooleanArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.BooleanArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Byte>

public fun kotlin.ByteArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Char>

public fun kotlin.CharArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Double>

public fun kotlin.DoubleArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Float>

public fun kotlin.FloatArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Int>

public fun kotlin.IntArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Long>

public fun kotlin.LongArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.Short>

public fun kotlin.ShortArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<kotlin.UShort>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.List<T>.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.List<T>

public fun <T> kotlin.collections.List<T>.slice(indices: kotlin.ranges.IntRange): kotlin.collections.List<T>

public fun <T> kotlin.Array<T>.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.Array<T>

public fun <T> kotlin.Array<T>.sliceArray(indices: kotlin.ranges.IntRange): kotlin.Array<T>

public fun kotlin.BooleanArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.BooleanArray

public fun kotlin.BooleanArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.BooleanArray

public fun kotlin.ByteArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.ByteArray

public fun kotlin.ByteArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.ByteArray

public fun kotlin.CharArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.CharArray

public fun kotlin.CharArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.CharArray

public fun kotlin.DoubleArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.DoubleArray

public fun kotlin.DoubleArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.DoubleArray

public fun kotlin.FloatArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.FloatArray

public fun kotlin.FloatArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.FloatArray

public fun kotlin.IntArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.IntArray

public fun kotlin.IntArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.IntArray

public fun kotlin.LongArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.LongArray

public fun kotlin.LongArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.LongArray

public fun kotlin.ShortArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.ShortArray

public fun kotlin.ShortArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sliceArray(indices: kotlin.collections.Collection<kotlin.Int>): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sliceArray(indices: kotlin.ranges.IntRange): kotlin.UShortArray

public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sort(): kotlin.Unit

public fun <T> kotlin.Array<out T>.sort(comparison: (a: T, b: T) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.ByteArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sort(noinline comparison: (a: kotlin.Byte, b: kotlin.Byte) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.CharArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sort(noinline comparison: (a: kotlin.Char, b: kotlin.Char) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.DoubleArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sort(noinline comparison: (a: kotlin.Double, b: kotlin.Double) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.FloatArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sort(noinline comparison: (a: kotlin.Float, b: kotlin.Float) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.IntArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sort(noinline comparison: (a: kotlin.Int, b: kotlin.Int) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.LongArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sort(noinline comparison: (a: kotlin.Long, b: kotlin.Long) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun kotlin.ShortArray.sort(): kotlin.Unit

@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sort(noinline comparison: (a: kotlin.Short, b: kotlin.Short) -> kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sort(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sort(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sort(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sort(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sort(fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun <T : kotlin.Comparable<T>> kotlin.collections.MutableList<T>.sort(): kotlin.Unit

public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.sortBy(crossinline selector: (T) -> R?): kotlin.Unit

public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.MutableList<T>.sortBy(crossinline selector: (T) -> R?): kotlin.Unit

public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.sortByDescending(crossinline selector: (T) -> R?): kotlin.Unit

public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.MutableList<T>.sortByDescending(crossinline selector: (T) -> R?): kotlin.Unit

public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.ByteArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ByteArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.CharArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.DoubleArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.DoubleArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.FloatArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.FloatArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.IntArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.IntArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.LongArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.LongArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun kotlin.ShortArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.ShortArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sortDescending(): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sortDescending(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

public fun <T : kotlin.Comparable<T>> kotlin.collections.MutableList<T>.sortDescending(): kotlin.Unit

public fun <T> kotlin.Array<out T>.sortWith(comparator: kotlin.Comparator<in T>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.Array<out T>.sortWith(comparator: kotlin.Comparator<in T>, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.Unit

public fun <T> kotlin.collections.MutableList<T>.sortWith(comparator: kotlin.Comparator<in T>): kotlin.Unit

public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sorted(): kotlin.collections.List<T>

public fun kotlin.ByteArray.sorted(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.sorted(): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.sorted(): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.sorted(): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.sorted(): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.sorted(): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.sorted(): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sorted(): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sorted(): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sorted(): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sorted(): kotlin.collections.List<kotlin.UShort>

public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.sorted(): kotlin.collections.List<T>

public fun <T : kotlin.Comparable<T>> kotlin.Array<T>.sortedArray(): kotlin.Array<T>

public fun kotlin.ByteArray.sortedArray(): kotlin.ByteArray

public fun kotlin.CharArray.sortedArray(): kotlin.CharArray

public fun kotlin.DoubleArray.sortedArray(): kotlin.DoubleArray

public fun kotlin.FloatArray.sortedArray(): kotlin.FloatArray

public fun kotlin.IntArray.sortedArray(): kotlin.IntArray

public fun kotlin.LongArray.sortedArray(): kotlin.LongArray

public fun kotlin.ShortArray.sortedArray(): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sortedArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sortedArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sortedArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sortedArray(): kotlin.UShortArray

public fun <T : kotlin.Comparable<T>> kotlin.Array<T>.sortedArrayDescending(): kotlin.Array<T>

public fun kotlin.ByteArray.sortedArrayDescending(): kotlin.ByteArray

public fun kotlin.CharArray.sortedArrayDescending(): kotlin.CharArray

public fun kotlin.DoubleArray.sortedArrayDescending(): kotlin.DoubleArray

public fun kotlin.FloatArray.sortedArrayDescending(): kotlin.FloatArray

public fun kotlin.IntArray.sortedArrayDescending(): kotlin.IntArray

public fun kotlin.LongArray.sortedArrayDescending(): kotlin.LongArray

public fun kotlin.ShortArray.sortedArrayDescending(): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sortedArrayDescending(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sortedArrayDescending(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sortedArrayDescending(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sortedArrayDescending(): kotlin.UShortArray

public fun <T> kotlin.Array<out T>.sortedArrayWith(comparator: kotlin.Comparator<in T>): kotlin.Array<out T>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.sortedBy(crossinline selector: (T) -> R?): kotlin.collections.List<T>

public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.sortedBy(crossinline selector: (kotlin.Boolean) -> R?): kotlin.collections.List<kotlin.Boolean>

public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.sortedBy(crossinline selector: (kotlin.Byte) -> R?): kotlin.collections.List<kotlin.Byte>

public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.sortedBy(crossinline selector: (kotlin.Char) -> R?): kotlin.collections.List<kotlin.Char>

public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.sortedBy(crossinline selector: (kotlin.Double) -> R?): kotlin.collections.List<kotlin.Double>

public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.sortedBy(crossinline selector: (kotlin.Float) -> R?): kotlin.collections.List<kotlin.Float>

public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.sortedBy(crossinline selector: (kotlin.Int) -> R?): kotlin.collections.List<kotlin.Int>

public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.sortedBy(crossinline selector: (kotlin.Long) -> R?): kotlin.collections.List<kotlin.Long>

public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.sortedBy(crossinline selector: (kotlin.Short) -> R?): kotlin.collections.List<kotlin.Short>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.sortedBy(crossinline selector: (T) -> R?): kotlin.collections.List<T>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.Array<out T>.sortedByDescending(crossinline selector: (T) -> R?): kotlin.collections.List<T>

public inline fun <R : kotlin.Comparable<R>> kotlin.BooleanArray.sortedByDescending(crossinline selector: (kotlin.Boolean) -> R?): kotlin.collections.List<kotlin.Boolean>

public inline fun <R : kotlin.Comparable<R>> kotlin.ByteArray.sortedByDescending(crossinline selector: (kotlin.Byte) -> R?): kotlin.collections.List<kotlin.Byte>

public inline fun <R : kotlin.Comparable<R>> kotlin.CharArray.sortedByDescending(crossinline selector: (kotlin.Char) -> R?): kotlin.collections.List<kotlin.Char>

public inline fun <R : kotlin.Comparable<R>> kotlin.DoubleArray.sortedByDescending(crossinline selector: (kotlin.Double) -> R?): kotlin.collections.List<kotlin.Double>

public inline fun <R : kotlin.Comparable<R>> kotlin.FloatArray.sortedByDescending(crossinline selector: (kotlin.Float) -> R?): kotlin.collections.List<kotlin.Float>

public inline fun <R : kotlin.Comparable<R>> kotlin.IntArray.sortedByDescending(crossinline selector: (kotlin.Int) -> R?): kotlin.collections.List<kotlin.Int>

public inline fun <R : kotlin.Comparable<R>> kotlin.LongArray.sortedByDescending(crossinline selector: (kotlin.Long) -> R?): kotlin.collections.List<kotlin.Long>

public inline fun <R : kotlin.Comparable<R>> kotlin.ShortArray.sortedByDescending(crossinline selector: (kotlin.Short) -> R?): kotlin.collections.List<kotlin.Short>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.sortedByDescending(crossinline selector: (T) -> R?): kotlin.collections.List<T>

public fun <T : kotlin.Comparable<T>> kotlin.Array<out T>.sortedDescending(): kotlin.collections.List<T>

public fun kotlin.ByteArray.sortedDescending(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.sortedDescending(): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.sortedDescending(): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.sortedDescending(): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.sortedDescending(): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.sortedDescending(): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.sortedDescending(): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.sortedDescending(): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.sortedDescending(): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.sortedDescending(): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.sortedDescending(): kotlin.collections.List<kotlin.UShort>

public fun <T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.sortedDescending(): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.sortedWith(comparator: kotlin.Comparator<in T>): kotlin.collections.List<T>

public fun kotlin.BooleanArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Double>): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Float>): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Int>): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Long>): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.sortedWith(comparator: kotlin.Comparator<in kotlin.Short>): kotlin.collections.List<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.sortedWith(comparator: kotlin.Comparator<in T>): kotlin.collections.List<T>

public infix fun <T> kotlin.Array<out T>.subtract(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public infix fun kotlin.BooleanArray.subtract(other: kotlin.collections.Iterable<kotlin.Boolean>): kotlin.collections.Set<kotlin.Boolean>

public infix fun kotlin.ByteArray.subtract(other: kotlin.collections.Iterable<kotlin.Byte>): kotlin.collections.Set<kotlin.Byte>

public infix fun kotlin.CharArray.subtract(other: kotlin.collections.Iterable<kotlin.Char>): kotlin.collections.Set<kotlin.Char>

public infix fun kotlin.DoubleArray.subtract(other: kotlin.collections.Iterable<kotlin.Double>): kotlin.collections.Set<kotlin.Double>

public infix fun kotlin.FloatArray.subtract(other: kotlin.collections.Iterable<kotlin.Float>): kotlin.collections.Set<kotlin.Float>

public infix fun kotlin.IntArray.subtract(other: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.Set<kotlin.Int>

public infix fun kotlin.LongArray.subtract(other: kotlin.collections.Iterable<kotlin.Long>): kotlin.collections.Set<kotlin.Long>

public infix fun kotlin.ShortArray.subtract(other: kotlin.collections.Iterable<kotlin.Short>): kotlin.collections.Set<kotlin.Short>

public infix fun <T> kotlin.collections.Iterable<T>.subtract(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

@kotlin.jvm.JvmName(name = "sumOfByte")
public fun kotlin.Array<out kotlin.Byte>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfDouble")
public fun kotlin.Array<out kotlin.Double>.sum(): kotlin.Double

@kotlin.jvm.JvmName(name = "sumOfFloat")
public fun kotlin.Array<out kotlin.Float>.sum(): kotlin.Float

@kotlin.jvm.JvmName(name = "sumOfInt")
public fun kotlin.Array<out kotlin.Int>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfLong")
public fun kotlin.Array<out kotlin.Long>.sum(): kotlin.Long

@kotlin.jvm.JvmName(name = "sumOfShort")
public fun kotlin.Array<out kotlin.Short>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfUByte")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UByte>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UInt>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.ULong>.sum(): kotlin.ULong

@kotlin.jvm.JvmName(name = "sumOfUShort")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UShort>.sum(): kotlin.UInt

public fun kotlin.ByteArray.sum(): kotlin.Int

public fun kotlin.DoubleArray.sum(): kotlin.Double

public fun kotlin.FloatArray.sum(): kotlin.Float

public fun kotlin.IntArray.sum(): kotlin.Int

public fun kotlin.LongArray.sum(): kotlin.Long

public fun kotlin.ShortArray.sum(): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sum(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sum(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sum(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfByte")
public fun kotlin.collections.Iterable<kotlin.Byte>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfDouble")
public fun kotlin.collections.Iterable<kotlin.Double>.sum(): kotlin.Double

@kotlin.jvm.JvmName(name = "sumOfFloat")
public fun kotlin.collections.Iterable<kotlin.Float>.sum(): kotlin.Float

@kotlin.jvm.JvmName(name = "sumOfInt")
public fun kotlin.collections.Iterable<kotlin.Int>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfLong")
public fun kotlin.collections.Iterable<kotlin.Long>.sum(): kotlin.Long

@kotlin.jvm.JvmName(name = "sumOfShort")
public fun kotlin.collections.Iterable<kotlin.Short>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfUByte")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Iterable<kotlin.UByte>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Iterable<kotlin.UInt>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Iterable<kotlin.ULong>.sum(): kotlin.ULong

@kotlin.jvm.JvmName(name = "sumOfUShort")
@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Iterable<kotlin.UShort>.sum(): kotlin.UInt

public inline fun <T> kotlin.Array<out T>.sumBy(selector: (T) -> kotlin.Int): kotlin.Int

public inline fun kotlin.BooleanArray.sumBy(selector: (kotlin.Boolean) -> kotlin.Int): kotlin.Int

public inline fun kotlin.ByteArray.sumBy(selector: (kotlin.Byte) -> kotlin.Int): kotlin.Int

public inline fun kotlin.CharArray.sumBy(selector: (kotlin.Char) -> kotlin.Int): kotlin.Int

public inline fun kotlin.DoubleArray.sumBy(selector: (kotlin.Double) -> kotlin.Int): kotlin.Int

public inline fun kotlin.FloatArray.sumBy(selector: (kotlin.Float) -> kotlin.Int): kotlin.Int

public inline fun kotlin.IntArray.sumBy(selector: (kotlin.Int) -> kotlin.Int): kotlin.Int

public inline fun kotlin.LongArray.sumBy(selector: (kotlin.Long) -> kotlin.Int): kotlin.Int

public inline fun kotlin.ShortArray.sumBy(selector: (kotlin.Short) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumBy(selector: (kotlin.UByte) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumBy(selector: (kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumBy(selector: (kotlin.ULong) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumBy(selector: (kotlin.UShort) -> kotlin.UInt): kotlin.UInt

public inline fun <T> kotlin.collections.Iterable<T>.sumBy(selector: (T) -> kotlin.Int): kotlin.Int

public inline fun <T> kotlin.Array<out T>.sumByDouble(selector: (T) -> kotlin.Double): kotlin.Double

public inline fun kotlin.BooleanArray.sumByDouble(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double

public inline fun kotlin.ByteArray.sumByDouble(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double

public inline fun kotlin.CharArray.sumByDouble(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

public inline fun kotlin.DoubleArray.sumByDouble(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double

public inline fun kotlin.FloatArray.sumByDouble(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double

public inline fun kotlin.IntArray.sumByDouble(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double

public inline fun kotlin.LongArray.sumByDouble(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double

public inline fun kotlin.ShortArray.sumByDouble(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumByDouble(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumByDouble(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumByDouble(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumByDouble(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double

public inline fun <T> kotlin.collections.Iterable<T>.sumByDouble(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.sumOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.sumOf(selector: (T) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.sumOf(selector: (T) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.sumOf(selector: (T) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.Array<out T>.sumOf(selector: (T) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.sumOf(selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.sumOf(selector: (kotlin.Boolean) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.sumOf(selector: (kotlin.Boolean) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.sumOf(selector: (kotlin.Boolean) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.BooleanArray.sumOf(selector: (kotlin.Boolean) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sumOf(selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sumOf(selector: (kotlin.Byte) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sumOf(selector: (kotlin.Byte) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sumOf(selector: (kotlin.Byte) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.sumOf(selector: (kotlin.Byte) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sumOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sumOf(selector: (kotlin.Char) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sumOf(selector: (kotlin.Char) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sumOf(selector: (kotlin.Char) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.CharArray.sumOf(selector: (kotlin.Char) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sumOf(selector: (kotlin.Double) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sumOf(selector: (kotlin.Double) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sumOf(selector: (kotlin.Double) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sumOf(selector: (kotlin.Double) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.DoubleArray.sumOf(selector: (kotlin.Double) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sumOf(selector: (kotlin.Float) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sumOf(selector: (kotlin.Float) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sumOf(selector: (kotlin.Float) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sumOf(selector: (kotlin.Float) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.FloatArray.sumOf(selector: (kotlin.Float) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sumOf(selector: (kotlin.Int) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sumOf(selector: (kotlin.Int) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sumOf(selector: (kotlin.Int) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sumOf(selector: (kotlin.Int) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.sumOf(selector: (kotlin.Int) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sumOf(selector: (kotlin.Long) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sumOf(selector: (kotlin.Long) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sumOf(selector: (kotlin.Long) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sumOf(selector: (kotlin.Long) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.sumOf(selector: (kotlin.Long) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sumOf(selector: (kotlin.Short) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sumOf(selector: (kotlin.Short) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sumOf(selector: (kotlin.Short) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sumOf(selector: (kotlin.Short) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.sumOf(selector: (kotlin.Short) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumOf(selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumOf(selector: (kotlin.UByte) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumOf(selector: (kotlin.UByte) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumOf(selector: (kotlin.UByte) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.sumOf(selector: (kotlin.UByte) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumOf(selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumOf(selector: (kotlin.UInt) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumOf(selector: (kotlin.UInt) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumOf(selector: (kotlin.UInt) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.sumOf(selector: (kotlin.UInt) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumOf(selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumOf(selector: (kotlin.ULong) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumOf(selector: (kotlin.ULong) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumOf(selector: (kotlin.ULong) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.sumOf(selector: (kotlin.ULong) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumOf(selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumOf(selector: (kotlin.UShort) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumOf(selector: (kotlin.UShort) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumOf(selector: (kotlin.UShort) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.sumOf(selector: (kotlin.UShort) -> kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.sumOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.sumOf(selector: (T) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.sumOf(selector: (T) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.sumOf(selector: (T) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Iterable<T>.sumOf(selector: (T) -> kotlin.ULong): kotlin.ULong

public fun <T> kotlin.Array<out T>.take(n: kotlin.Int): kotlin.collections.List<T>

public fun kotlin.BooleanArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.take(n: kotlin.Int): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.Iterable<T>.take(n: kotlin.Int): kotlin.collections.List<T>

public fun <T> kotlin.Array<out T>.takeLast(n: kotlin.Int): kotlin.collections.List<T>

public fun kotlin.BooleanArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.takeLast(n: kotlin.Int): kotlin.collections.List<kotlin.UShort>

public fun <T> kotlin.collections.List<T>.takeLast(n: kotlin.Int): kotlin.collections.List<T>

public inline fun <T> kotlin.Array<out T>.takeLastWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.takeLastWhile(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.takeLastWhile(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.takeLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.takeLastWhile(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.takeLastWhile(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.takeLastWhile(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.takeLastWhile(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.takeLastWhile(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.takeLastWhile(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.takeLastWhile(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.takeLastWhile(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.takeLastWhile(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.List<T>.takeLastWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun <T> kotlin.Array<out T>.takeWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public inline fun kotlin.BooleanArray.takeWhile(predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.collections.List<kotlin.Boolean>

public inline fun kotlin.ByteArray.takeWhile(predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.collections.List<kotlin.Byte>

public inline fun kotlin.CharArray.takeWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.collections.List<kotlin.Char>

public inline fun kotlin.DoubleArray.takeWhile(predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.collections.List<kotlin.Double>

public inline fun kotlin.FloatArray.takeWhile(predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.collections.List<kotlin.Float>

public inline fun kotlin.IntArray.takeWhile(predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.collections.List<kotlin.Int>

public inline fun kotlin.LongArray.takeWhile(predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.collections.List<kotlin.Long>

public inline fun kotlin.ShortArray.takeWhile(predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.collections.List<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.takeWhile(predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.collections.List<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.takeWhile(predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.collections.List<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.takeWhile(predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.collections.List<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.takeWhile(predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.collections.List<kotlin.UShort>

public inline fun <T> kotlin.collections.Iterable<T>.takeWhile(predicate: (T) -> kotlin.Boolean): kotlin.collections.List<T>

public fun kotlin.Array<out kotlin.Boolean>.toBooleanArray(): kotlin.BooleanArray

public fun kotlin.collections.Collection<kotlin.Boolean>.toBooleanArray(): kotlin.BooleanArray

public fun kotlin.Array<out kotlin.Byte>.toByteArray(): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.toByteArray(): kotlin.ByteArray

public fun kotlin.collections.Collection<kotlin.Byte>.toByteArray(): kotlin.ByteArray

public fun kotlin.Array<out kotlin.Char>.toCharArray(): kotlin.CharArray

public fun kotlin.collections.Collection<kotlin.Char>.toCharArray(): kotlin.CharArray

public fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.Array<out T>.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Boolean>> kotlin.BooleanArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Byte>> kotlin.ByteArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Double>> kotlin.DoubleArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Float>> kotlin.FloatArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Int>> kotlin.IntArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Long>> kotlin.LongArray.toCollection(destination: C): C

public fun <C : kotlin.collections.MutableCollection<in kotlin.Short>> kotlin.ShortArray.toCollection(destination: C): C

public fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.collections.Iterable<T>.toCollection(destination: C): C

public fun kotlin.Array<out kotlin.Double>.toDoubleArray(): kotlin.DoubleArray

public fun kotlin.collections.Collection<kotlin.Double>.toDoubleArray(): kotlin.DoubleArray

public fun kotlin.Array<out kotlin.Float>.toFloatArray(): kotlin.FloatArray

public fun kotlin.collections.Collection<kotlin.Float>.toFloatArray(): kotlin.FloatArray

public fun <T> kotlin.Array<out T>.toHashSet(): kotlin.collections.HashSet<T>

public fun kotlin.BooleanArray.toHashSet(): kotlin.collections.HashSet<kotlin.Boolean>

public fun kotlin.ByteArray.toHashSet(): kotlin.collections.HashSet<kotlin.Byte>

public fun kotlin.CharArray.toHashSet(): kotlin.collections.HashSet<kotlin.Char>

public fun kotlin.DoubleArray.toHashSet(): kotlin.collections.HashSet<kotlin.Double>

public fun kotlin.FloatArray.toHashSet(): kotlin.collections.HashSet<kotlin.Float>

public fun kotlin.IntArray.toHashSet(): kotlin.collections.HashSet<kotlin.Int>

public fun kotlin.LongArray.toHashSet(): kotlin.collections.HashSet<kotlin.Long>

public fun kotlin.ShortArray.toHashSet(): kotlin.collections.HashSet<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.toHashSet(): kotlin.collections.HashSet<T>

public fun kotlin.Array<out kotlin.Int>.toIntArray(): kotlin.IntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UIntArray.toIntArray(): kotlin.IntArray

public fun kotlin.collections.Collection<kotlin.Int>.toIntArray(): kotlin.IntArray

public fun <T> kotlin.Array<out T>.toList(): kotlin.collections.List<T>

public fun kotlin.BooleanArray.toList(): kotlin.collections.List<kotlin.Boolean>

public fun kotlin.ByteArray.toList(): kotlin.collections.List<kotlin.Byte>

public fun kotlin.CharArray.toList(): kotlin.collections.List<kotlin.Char>

public fun kotlin.DoubleArray.toList(): kotlin.collections.List<kotlin.Double>

public fun kotlin.FloatArray.toList(): kotlin.collections.List<kotlin.Float>

public fun kotlin.IntArray.toList(): kotlin.collections.List<kotlin.Int>

public fun kotlin.LongArray.toList(): kotlin.collections.List<kotlin.Long>

public fun kotlin.ShortArray.toList(): kotlin.collections.List<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.toList(): kotlin.collections.List<T>

public fun <K, V> kotlin.collections.Map<out K, V>.toList(): kotlin.collections.List<kotlin.Pair<K, V>>

public fun kotlin.Array<out kotlin.Long>.toLongArray(): kotlin.LongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ULongArray.toLongArray(): kotlin.LongArray

public fun kotlin.collections.Collection<kotlin.Long>.toLongArray(): kotlin.LongArray

public fun <K, V> kotlin.Array<out kotlin.Pair<K, V>>.toMap(): kotlin.collections.Map<K, V>

public fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.Array<out kotlin.Pair<K, V>>.toMap(destination: M): M

public fun <K, V> kotlin.collections.Iterable<kotlin.Pair<K, V>>.toMap(): kotlin.collections.Map<K, V>

public fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Iterable<kotlin.Pair<K, V>>.toMap(destination: M): M

@kotlin.SinceKotlin(version = "1.1")
public fun <K, V> kotlin.collections.Map<out K, V>.toMap(): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.1")
public fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.collections.Map<out K, V>.toMap(destination: M): M

public fun <K, V> kotlin.sequences.Sequence<kotlin.Pair<K, V>>.toMap(): kotlin.collections.Map<K, V>

public fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<kotlin.Pair<K, V>>.toMap(destination: M): M

public fun <T> kotlin.Array<out T>.toMutableList(): kotlin.collections.MutableList<T>

public fun kotlin.BooleanArray.toMutableList(): kotlin.collections.MutableList<kotlin.Boolean>

public fun kotlin.ByteArray.toMutableList(): kotlin.collections.MutableList<kotlin.Byte>

public fun kotlin.CharArray.toMutableList(): kotlin.collections.MutableList<kotlin.Char>

public fun kotlin.DoubleArray.toMutableList(): kotlin.collections.MutableList<kotlin.Double>

public fun kotlin.FloatArray.toMutableList(): kotlin.collections.MutableList<kotlin.Float>

public fun kotlin.IntArray.toMutableList(): kotlin.collections.MutableList<kotlin.Int>

public fun kotlin.LongArray.toMutableList(): kotlin.collections.MutableList<kotlin.Long>

public fun kotlin.ShortArray.toMutableList(): kotlin.collections.MutableList<kotlin.Short>

public fun <T> kotlin.collections.Collection<T>.toMutableList(): kotlin.collections.MutableList<T>

public fun <T> kotlin.collections.Iterable<T>.toMutableList(): kotlin.collections.MutableList<T>

@kotlin.SinceKotlin(version = "1.1")
public fun <K, V> kotlin.collections.Map<out K, V>.toMutableMap(): kotlin.collections.MutableMap<K, V>

public fun <T> kotlin.Array<out T>.toMutableSet(): kotlin.collections.MutableSet<T>

public fun kotlin.BooleanArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Boolean>

public fun kotlin.ByteArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Byte>

public fun kotlin.CharArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Char>

public fun kotlin.DoubleArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Double>

public fun kotlin.FloatArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Float>

public fun kotlin.IntArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Int>

public fun kotlin.LongArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Long>

public fun kotlin.ShortArray.toMutableSet(): kotlin.collections.MutableSet<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.toMutableSet(): kotlin.collections.MutableSet<T>

@kotlin.internal.InlineOnly
public inline fun <K, V> kotlin.collections.Map.Entry<K, V>.toPair(): kotlin.Pair<K, V>

public fun <T> kotlin.Array<out T>.toSet(): kotlin.collections.Set<T>

public fun kotlin.BooleanArray.toSet(): kotlin.collections.Set<kotlin.Boolean>

public fun kotlin.ByteArray.toSet(): kotlin.collections.Set<kotlin.Byte>

public fun kotlin.CharArray.toSet(): kotlin.collections.Set<kotlin.Char>

public fun kotlin.DoubleArray.toSet(): kotlin.collections.Set<kotlin.Double>

public fun kotlin.FloatArray.toSet(): kotlin.collections.Set<kotlin.Float>

public fun kotlin.IntArray.toSet(): kotlin.collections.Set<kotlin.Int>

public fun kotlin.LongArray.toSet(): kotlin.collections.Set<kotlin.Long>

public fun kotlin.ShortArray.toSet(): kotlin.collections.Set<kotlin.Short>

public fun <T> kotlin.collections.Iterable<T>.toSet(): kotlin.collections.Set<T>

public fun kotlin.Array<out kotlin.Short>.toShortArray(): kotlin.ShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UShortArray.toShortArray(): kotlin.ShortArray

public fun kotlin.collections.Collection<kotlin.Short>.toShortArray(): kotlin.ShortArray

public fun kotlin.BooleanArray.toTypedArray(): kotlin.Array<kotlin.Boolean>

public fun kotlin.ByteArray.toTypedArray(): kotlin.Array<kotlin.Byte>

public fun kotlin.CharArray.toTypedArray(): kotlin.Array<kotlin.Char>

public fun kotlin.DoubleArray.toTypedArray(): kotlin.Array<kotlin.Double>

public fun kotlin.FloatArray.toTypedArray(): kotlin.Array<kotlin.Float>

public fun kotlin.IntArray.toTypedArray(): kotlin.Array<kotlin.Int>

public fun kotlin.LongArray.toTypedArray(): kotlin.Array<kotlin.Long>

public fun kotlin.ShortArray.toTypedArray(): kotlin.Array<kotlin.Short>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.toTypedArray(): kotlin.Array<kotlin.UByte>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.toTypedArray(): kotlin.Array<kotlin.UInt>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.toTypedArray(): kotlin.Array<kotlin.ULong>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.toTypedArray(): kotlin.Array<kotlin.UShort>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.collections.Collection<T>.toTypedArray(): kotlin.Array<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UByte>.toUByteArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ByteArray.toUByteArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Collection<kotlin.UByte>.toUByteArray(): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UInt>.toUIntArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.IntArray.toUIntArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Collection<kotlin.UInt>.toUIntArray(): kotlin.UIntArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.ULong>.toULongArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.LongArray.toULongArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Collection<kotlin.ULong>.toULongArray(): kotlin.ULongArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.Array<out kotlin.UShort>.toUShortArray(): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.ShortArray.toUShortArray(): kotlin.UShortArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.collections.Collection<kotlin.UShort>.toUShortArray(): kotlin.UShortArray

public infix fun <T> kotlin.Array<out T>.union(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public infix fun kotlin.BooleanArray.union(other: kotlin.collections.Iterable<kotlin.Boolean>): kotlin.collections.Set<kotlin.Boolean>

public infix fun kotlin.ByteArray.union(other: kotlin.collections.Iterable<kotlin.Byte>): kotlin.collections.Set<kotlin.Byte>

public infix fun kotlin.CharArray.union(other: kotlin.collections.Iterable<kotlin.Char>): kotlin.collections.Set<kotlin.Char>

public infix fun kotlin.DoubleArray.union(other: kotlin.collections.Iterable<kotlin.Double>): kotlin.collections.Set<kotlin.Double>

public infix fun kotlin.FloatArray.union(other: kotlin.collections.Iterable<kotlin.Float>): kotlin.collections.Set<kotlin.Float>

public infix fun kotlin.IntArray.union(other: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.Set<kotlin.Int>

public infix fun kotlin.LongArray.union(other: kotlin.collections.Iterable<kotlin.Long>): kotlin.collections.Set<kotlin.Long>

public infix fun kotlin.ShortArray.union(other: kotlin.collections.Iterable<kotlin.Short>): kotlin.collections.Set<kotlin.Short>

public infix fun <T> kotlin.collections.Iterable<T>.union(other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>

public fun <T, R> kotlin.Array<out kotlin.Pair<T, R>>.unzip(): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<R>>

public fun <T, R> kotlin.collections.Iterable<kotlin.Pair<T, R>>.unzip(): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<R>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.Iterable<T>.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ...): kotlin.collections.List<kotlin.collections.List<T>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T, R> kotlin.collections.Iterable<T>.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ..., transform: (kotlin.collections.List<T>) -> R): kotlin.collections.List<R>

public fun <K, V> kotlin.collections.Map<K, V>.withDefault(defaultValue: (key: K) -> V): kotlin.collections.Map<K, V>

@kotlin.jvm.JvmName(name = "withDefaultMutable")
public fun <K, V> kotlin.collections.MutableMap<K, V>.withDefault(defaultValue: (key: K) -> V): kotlin.collections.MutableMap<K, V>

public fun <T> kotlin.Array<out T>.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<T>>

public fun kotlin.BooleanArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Boolean>>

public fun kotlin.ByteArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Byte>>

public fun kotlin.CharArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Char>>

public fun kotlin.DoubleArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Double>>

public fun kotlin.FloatArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Float>>

public fun kotlin.IntArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Int>>

public fun kotlin.LongArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Long>>

public fun kotlin.ShortArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Short>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UByteArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.UByte>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UIntArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.UInt>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.ULongArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.ULong>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.UShortArray.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.UShort>>

public fun <T> kotlin.collections.Iterable<T>.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<T>>

public fun <T> kotlin.collections.Iterator<T>.withIndex(): kotlin.collections.Iterator<kotlin.collections.IndexedValue<T>>

public infix fun <T, R> kotlin.Array<out T>.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<T, R>>

public inline fun <T, R, V> kotlin.Array<out T>.zip(other: kotlin.Array<out R>, transform: (a: T, b: R) -> V): kotlin.collections.List<V>

public infix fun <T, R> kotlin.Array<out T>.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<T, R>>

public inline fun <T, R, V> kotlin.Array<out T>.zip(other: kotlin.collections.Iterable<R>, transform: (a: T, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.BooleanArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Boolean, R>>

public inline fun <R, V> kotlin.BooleanArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Boolean, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.BooleanArray.zip(other: kotlin.BooleanArray): kotlin.collections.List<kotlin.Pair<kotlin.Boolean, kotlin.Boolean>>

public inline fun <V> kotlin.BooleanArray.zip(other: kotlin.BooleanArray, transform: (a: kotlin.Boolean, b: kotlin.Boolean) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.BooleanArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Boolean, R>>

public inline fun <R, V> kotlin.BooleanArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Boolean, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.ByteArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Byte, R>>

public inline fun <R, V> kotlin.ByteArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Byte, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.ByteArray.zip(other: kotlin.ByteArray): kotlin.collections.List<kotlin.Pair<kotlin.Byte, kotlin.Byte>>

public inline fun <V> kotlin.ByteArray.zip(other: kotlin.ByteArray, transform: (a: kotlin.Byte, b: kotlin.Byte) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.ByteArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Byte, R>>

public inline fun <R, V> kotlin.ByteArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Byte, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.CharArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Char, R>>

public inline fun <R, V> kotlin.CharArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Char, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.CharArray.zip(other: kotlin.CharArray): kotlin.collections.List<kotlin.Pair<kotlin.Char, kotlin.Char>>

public inline fun <V> kotlin.CharArray.zip(other: kotlin.CharArray, transform: (a: kotlin.Char, b: kotlin.Char) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.CharArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Char, R>>

public inline fun <R, V> kotlin.CharArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Char, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.DoubleArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Double, R>>

public inline fun <R, V> kotlin.DoubleArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Double, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.DoubleArray.zip(other: kotlin.DoubleArray): kotlin.collections.List<kotlin.Pair<kotlin.Double, kotlin.Double>>

public inline fun <V> kotlin.DoubleArray.zip(other: kotlin.DoubleArray, transform: (a: kotlin.Double, b: kotlin.Double) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.DoubleArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Double, R>>

public inline fun <R, V> kotlin.DoubleArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Double, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.FloatArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Float, R>>

public inline fun <R, V> kotlin.FloatArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Float, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.FloatArray.zip(other: kotlin.FloatArray): kotlin.collections.List<kotlin.Pair<kotlin.Float, kotlin.Float>>

public inline fun <V> kotlin.FloatArray.zip(other: kotlin.FloatArray, transform: (a: kotlin.Float, b: kotlin.Float) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.FloatArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Float, R>>

public inline fun <R, V> kotlin.FloatArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Float, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.IntArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Int, R>>

public inline fun <R, V> kotlin.IntArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Int, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.IntArray.zip(other: kotlin.IntArray): kotlin.collections.List<kotlin.Pair<kotlin.Int, kotlin.Int>>

public inline fun <V> kotlin.IntArray.zip(other: kotlin.IntArray, transform: (a: kotlin.Int, b: kotlin.Int) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.IntArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Int, R>>

public inline fun <R, V> kotlin.IntArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Int, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.LongArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Long, R>>

public inline fun <R, V> kotlin.LongArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Long, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.LongArray.zip(other: kotlin.LongArray): kotlin.collections.List<kotlin.Pair<kotlin.Long, kotlin.Long>>

public inline fun <V> kotlin.LongArray.zip(other: kotlin.LongArray, transform: (a: kotlin.Long, b: kotlin.Long) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.LongArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Long, R>>

public inline fun <R, V> kotlin.LongArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Long, b: R) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.ShortArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.Short, R>>

public inline fun <R, V> kotlin.ShortArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.Short, b: R) -> V): kotlin.collections.List<V>

public infix fun kotlin.ShortArray.zip(other: kotlin.ShortArray): kotlin.collections.List<kotlin.Pair<kotlin.Short, kotlin.Short>>

public inline fun <V> kotlin.ShortArray.zip(other: kotlin.ShortArray, transform: (a: kotlin.Short, b: kotlin.Short) -> V): kotlin.collections.List<V>

public infix fun <R> kotlin.ShortArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.Short, R>>

public inline fun <R, V> kotlin.ShortArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.Short, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UByteArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.UByte, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UByteArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.UByte, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UByteArray.zip(other: kotlin.UByteArray): kotlin.collections.List<kotlin.Pair<kotlin.UByte, kotlin.UByte>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UByteArray.zip(other: kotlin.UByteArray, transform: (a: kotlin.UByte, b: kotlin.UByte) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UByteArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.UByte, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UByteArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.UByte, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UIntArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.UInt, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UIntArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.UInt, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UIntArray.zip(other: kotlin.UIntArray): kotlin.collections.List<kotlin.Pair<kotlin.UInt, kotlin.UInt>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UIntArray.zip(other: kotlin.UIntArray, transform: (a: kotlin.UInt, b: kotlin.UInt) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UIntArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.UInt, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UIntArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.UInt, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.ULongArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.ULong, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.ULongArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.ULong, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.ULongArray.zip(other: kotlin.ULongArray): kotlin.collections.List<kotlin.Pair<kotlin.ULong, kotlin.ULong>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.ULongArray.zip(other: kotlin.ULongArray, transform: (a: kotlin.ULong, b: kotlin.ULong) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.ULongArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.ULong, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.ULongArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.ULong, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UShortArray.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<kotlin.UShort, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UShortArray.zip(other: kotlin.Array<out R>, transform: (a: kotlin.UShort, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun kotlin.UShortArray.zip(other: kotlin.UShortArray): kotlin.collections.List<kotlin.Pair<kotlin.UShort, kotlin.UShort>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <V> kotlin.UShortArray.zip(other: kotlin.UShortArray, transform: (a: kotlin.UShort, b: kotlin.UShort) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public infix fun <R> kotlin.UShortArray.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<kotlin.UShort, R>>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun <R, V> kotlin.UShortArray.zip(other: kotlin.collections.Iterable<R>, transform: (a: kotlin.UShort, b: R) -> V): kotlin.collections.List<V>

public infix fun <T, R> kotlin.collections.Iterable<T>.zip(other: kotlin.Array<out R>): kotlin.collections.List<kotlin.Pair<T, R>>

public inline fun <T, R, V> kotlin.collections.Iterable<T>.zip(other: kotlin.Array<out R>, transform: (a: T, b: R) -> V): kotlin.collections.List<V>

public infix fun <T, R> kotlin.collections.Iterable<T>.zip(other: kotlin.collections.Iterable<R>): kotlin.collections.List<kotlin.Pair<T, R>>

public inline fun <T, R, V> kotlin.collections.Iterable<T>.zip(other: kotlin.collections.Iterable<R>, transform: (a: T, b: R) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.collections.Iterable<T>.zipWithNext(): kotlin.collections.List<kotlin.Pair<T, T>>

@kotlin.SinceKotlin(version = "1.2")
public inline fun <T, R> kotlin.collections.Iterable<T>.zipWithNext(transform: (a: T, b: T) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.1")
public abstract class AbstractCollection<out E> : kotlin.collections.Collection<E> {
    protected constructor AbstractCollection<out E>()

    public abstract override val size: kotlin.Int { get; }

    public open override operator fun contains(element: E): kotlin.Boolean

    public open override fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun isEmpty(): kotlin.Boolean

    public abstract override operator fun iterator(): kotlin.collections.Iterator<E>

    @kotlin.js.JsName(name = "toArray")
    protected open fun toArray(): kotlin.Array<kotlin.Any?>

    protected open fun <T> toArray(array: kotlin.Array<T>): kotlin.Array<T>

    public open override fun toString(): kotlin.String
}

public abstract class AbstractIterator<T> : kotlin.collections.Iterator<T> {
    public constructor AbstractIterator<T>()

    protected abstract fun computeNext(): kotlin.Unit

    protected final fun done(): kotlin.Unit

    public open override operator fun hasNext(): kotlin.Boolean

    public open override operator fun next(): T

    protected final fun setNext(value: T): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.1")
public abstract class AbstractList<out E> : kotlin.collections.AbstractCollection<E>, kotlin.collections.List<E> {
    protected constructor AbstractList<out E>()

    public abstract override val size: kotlin.Int { get; }

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public abstract override operator fun get(index: kotlin.Int): E

    public open override fun hashCode(): kotlin.Int

    public open override fun indexOf(element: E): kotlin.Int

    public open override operator fun iterator(): kotlin.collections.Iterator<E>

    public open override fun lastIndexOf(element: E): kotlin.Int

    public open override fun listIterator(): kotlin.collections.ListIterator<E>

    public open override fun listIterator(index: kotlin.Int): kotlin.collections.ListIterator<E>

    public open override fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.List<E>
}

@kotlin.SinceKotlin(version = "1.1")
public abstract class AbstractMap<K, out V> : kotlin.collections.Map<K, V> {
    protected constructor AbstractMap<K, out V>()

    public open override val keys: kotlin.collections.Set<K> { get; }

    public open override val size: kotlin.Int { get; }

    public open override val values: kotlin.collections.Collection<V> { get; }

    public open override fun containsKey(key: K): kotlin.Boolean

    public open override fun containsValue(value: V): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override operator fun get(key: K): V?

    public open override fun hashCode(): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public open override fun toString(): kotlin.String
}

public abstract class AbstractMutableCollection<E> : kotlin.collections.AbstractCollection<E>, kotlin.collections.MutableCollection<E> {
    protected constructor AbstractMutableCollection<E>()

    public abstract override fun add(element: E): kotlin.Boolean

    public open override fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun clear(): kotlin.Unit

    public open override fun remove(element: E): kotlin.Boolean

    public open override fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.js.JsName(name = "toJSON")
    public open fun toJSON(): kotlin.Any
}

public abstract class AbstractMutableList<E> : kotlin.collections.AbstractMutableCollection<E>, kotlin.collections.MutableList<E> {
    protected constructor AbstractMutableList<E>()

    protected final var modCount: kotlin.Int { get; set; }

    public open override fun add(element: E): kotlin.Boolean

    public abstract override fun add(index: kotlin.Int, element: E): kotlin.Unit

    public open override fun addAll(index: kotlin.Int, elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun clear(): kotlin.Unit

    public open override operator fun contains(element: E): kotlin.Boolean

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun indexOf(element: E): kotlin.Int

    public open override operator fun iterator(): kotlin.collections.MutableIterator<E>

    public open override fun lastIndexOf(element: E): kotlin.Int

    public open override fun listIterator(): kotlin.collections.MutableListIterator<E>

    public open override fun listIterator(index: kotlin.Int): kotlin.collections.MutableListIterator<E>

    public open override fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun removeAt(index: kotlin.Int): E

    protected open fun removeRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

    public open override fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override operator fun set(index: kotlin.Int, element: E): E

    public open override fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.MutableList<E>
}

public abstract class AbstractMutableMap<K, V> : kotlin.collections.AbstractMap<K, V>, kotlin.collections.MutableMap<K, V> {
    protected constructor AbstractMutableMap<K, V>()

    public open override val keys: kotlin.collections.MutableSet<K> { get; }

    public open override val values: kotlin.collections.MutableCollection<V> { get; }

    public open override fun clear(): kotlin.Unit

    public abstract override fun put(key: K, value: V): V?

    public open override fun putAll(from: kotlin.collections.Map<out K, V>): kotlin.Unit

    public open override fun remove(key: K): V?
}

public abstract class AbstractMutableSet<E> : kotlin.collections.AbstractMutableCollection<E>, kotlin.collections.MutableSet<E> {
    protected constructor AbstractMutableSet<E>()

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int
}

@kotlin.SinceKotlin(version = "1.1")
public abstract class AbstractSet<out E> : kotlin.collections.AbstractCollection<E>, kotlin.collections.Set<E> {
    protected constructor AbstractSet<out E>()

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int
}

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public final class ArrayDeque<E> : kotlin.collections.AbstractMutableList<E> {
    public constructor ArrayDeque<E>()

    public constructor ArrayDeque<E>(initialCapacity: kotlin.Int)

    public constructor ArrayDeque<E>(elements: kotlin.collections.Collection<E>)

    public open override var size: kotlin.Int { get; }

    public open override fun add(element: E): kotlin.Boolean

    public open override fun add(index: kotlin.Int, element: E): kotlin.Unit

    public open override fun addAll(index: kotlin.Int, elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public final fun addFirst(element: E): kotlin.Unit

    public final fun addLast(element: E): kotlin.Unit

    public open override fun clear(): kotlin.Unit

    public open override operator fun contains(element: E): kotlin.Boolean

    public final fun first(): E

    public final fun firstOrNull(): E?

    public open override operator fun get(index: kotlin.Int): E

    public open override fun indexOf(element: E): kotlin.Int

    public open override fun isEmpty(): kotlin.Boolean

    public final fun last(): E

    public open override fun lastIndexOf(element: E): kotlin.Int

    public final fun lastOrNull(): E?

    public open override fun remove(element: E): kotlin.Boolean

    public open override fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun removeAt(index: kotlin.Int): E

    public final fun removeFirst(): E

    public final fun removeFirstOrNull(): E?

    public final fun removeLast(): E

    public final fun removeLastOrNull(): E?

    public open override fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override operator fun set(index: kotlin.Int, element: E): E

    protected open override fun toArray(): kotlin.Array<kotlin.Any?>

    protected open override fun <T> toArray(array: kotlin.Array<T>): kotlin.Array<T>
}

public open class ArrayList<E> : kotlin.collections.AbstractMutableList<E>, kotlin.collections.MutableList<E>, kotlin.collections.RandomAccess {
    public constructor ArrayList<E>()

    public constructor ArrayList<E>(initialCapacity: kotlin.Int = ...)

    public constructor ArrayList<E>(elements: kotlin.collections.Collection<E>)

    public open override val size: kotlin.Int { get; }

    public open override fun add(element: E): kotlin.Boolean

    public open override fun add(index: kotlin.Int, element: E): kotlin.Unit

    public open override fun addAll(index: kotlin.Int, elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public open override fun clear(): kotlin.Unit

    public final fun ensureCapacity(minCapacity: kotlin.Int): kotlin.Unit

    public open override operator fun get(index: kotlin.Int): E

    public open override fun indexOf(element: E): kotlin.Int

    public open override fun lastIndexOf(element: E): kotlin.Int

    public open override fun remove(element: E): kotlin.Boolean

    public open override fun removeAt(index: kotlin.Int): E

    protected open override fun removeRange(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.Unit

    public open override operator fun set(index: kotlin.Int, element: E): E

    protected open override fun toArray(): kotlin.Array<kotlin.Any?>

    protected open override fun <T> toArray(array: kotlin.Array<T>): kotlin.Array<T>

    public open override fun toString(): kotlin.String

    public final fun trimToSize(): kotlin.Unit
}

public abstract class BooleanIterator : kotlin.collections.Iterator<kotlin.Boolean> {
    public constructor BooleanIterator()

    public final override operator fun next(): kotlin.Boolean

    public abstract fun nextBoolean(): kotlin.Boolean
}

public abstract class ByteIterator : kotlin.collections.Iterator<kotlin.Byte> {
    public constructor ByteIterator()

    public final override operator fun next(): kotlin.Byte

    public abstract fun nextByte(): kotlin.Byte
}

public abstract class CharIterator : kotlin.collections.Iterator<kotlin.Char> {
    public constructor CharIterator()

    public final override operator fun next(): kotlin.Char

    public abstract fun nextChar(): kotlin.Char
}

public interface Collection<out E> : kotlin.collections.Iterable<E> {
    public abstract val size: kotlin.Int { get; }

    public abstract operator fun contains(element: E): kotlin.Boolean

    public abstract fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun isEmpty(): kotlin.Boolean

    public abstract override operator fun iterator(): kotlin.collections.Iterator<E>
}

public abstract class DoubleIterator : kotlin.collections.Iterator<kotlin.Double> {
    public constructor DoubleIterator()

    public final override operator fun next(): kotlin.Double

    public abstract fun nextDouble(): kotlin.Double
}

public abstract class FloatIterator : kotlin.collections.Iterator<kotlin.Float> {
    public constructor FloatIterator()

    public final override operator fun next(): kotlin.Float

    public abstract fun nextFloat(): kotlin.Float
}

@kotlin.SinceKotlin(version = "1.1")
public interface Grouping<T, out K> {
    public abstract fun keyOf(element: T): K

    public abstract fun sourceIterator(): kotlin.collections.Iterator<T>
}

public open class HashMap<K, V> : kotlin.collections.AbstractMutableMap<K, V>, kotlin.collections.MutableMap<K, V> {
    public constructor HashMap<K, V>()

    public constructor HashMap<K, V>(initialCapacity: kotlin.Int)

    public constructor HashMap<K, V>(initialCapacity: kotlin.Int, loadFactor: kotlin.Float = ...)

    public constructor HashMap<K, V>(original: kotlin.collections.Map<out K, V>)

    public open override val entries: kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>> { get; }

    public open override val size: kotlin.Int { get; }

    public open override fun clear(): kotlin.Unit

    public open override fun containsKey(key: K): kotlin.Boolean

    public open override fun containsValue(value: V): kotlin.Boolean

    protected open fun createEntrySet(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>

    public open override operator fun get(key: K): V?

    public open override fun put(key: K, value: V): V?

    public open override fun remove(key: K): V?
}

public open class HashSet<E> : kotlin.collections.AbstractMutableSet<E>, kotlin.collections.MutableSet<E> {
    public constructor HashSet<E>()

    public constructor HashSet<E>(initialCapacity: kotlin.Int)

    public constructor HashSet<E>(initialCapacity: kotlin.Int, loadFactor: kotlin.Float = ...)

    public constructor HashSet<E>(elements: kotlin.collections.Collection<E>)

    public open override val size: kotlin.Int { get; }

    public open override fun add(element: E): kotlin.Boolean

    public open override fun clear(): kotlin.Unit

    public open override operator fun contains(element: E): kotlin.Boolean

    public open override fun isEmpty(): kotlin.Boolean

    public open override operator fun iterator(): kotlin.collections.MutableIterator<E>

    public open override fun remove(element: E): kotlin.Boolean
}

public final data class IndexedValue<out T> {
    public constructor IndexedValue<out T>(index: kotlin.Int, value: T)

    public final val index: kotlin.Int { get; }

    public final val value: T { get; }

    public final operator fun component1(): kotlin.Int

    public final operator fun component2(): T

    public final fun copy(index: kotlin.Int = ..., value: T = ...): kotlin.collections.IndexedValue<T>

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String
}

public abstract class IntIterator : kotlin.collections.Iterator<kotlin.Int> {
    public constructor IntIterator()

    public final override operator fun next(): kotlin.Int

    public abstract fun nextInt(): kotlin.Int
}

public interface Iterable<out T> {
    public abstract operator fun iterator(): kotlin.collections.Iterator<T>
}

public interface Iterator<out T> {
    public abstract operator fun hasNext(): kotlin.Boolean

    public abstract operator fun next(): T
}

public open class LinkedHashMap<K, V> : kotlin.collections.HashMap<K, V>, kotlin.collections.MutableMap<K, V> {
    public constructor LinkedHashMap<K, V>()

    public constructor LinkedHashMap<K, V>(initialCapacity: kotlin.Int)

    public constructor LinkedHashMap<K, V>(initialCapacity: kotlin.Int, loadFactor: kotlin.Float = ...)

    public constructor LinkedHashMap<K, V>(original: kotlin.collections.Map<out K, V>)

    public open override val size: kotlin.Int { get; }

    public open override fun clear(): kotlin.Unit

    public open override fun containsKey(key: K): kotlin.Boolean

    public open override fun containsValue(value: V): kotlin.Boolean

    protected open override fun createEntrySet(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>

    public open override operator fun get(key: K): V?

    public open override fun put(key: K, value: V): V?

    public open override fun remove(key: K): V?
}

public open class LinkedHashSet<E> : kotlin.collections.HashSet<E>, kotlin.collections.MutableSet<E> {
    public constructor LinkedHashSet<E>()

    public constructor LinkedHashSet<E>(initialCapacity: kotlin.Int)

    public constructor LinkedHashSet<E>(initialCapacity: kotlin.Int, loadFactor: kotlin.Float = ...)

    public constructor LinkedHashSet<E>(elements: kotlin.collections.Collection<E>)
}

public interface List<out E> : kotlin.collections.Collection<E> {
    public abstract override val size: kotlin.Int { get; }

    public abstract override operator fun contains(element: E): kotlin.Boolean

    public abstract override fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract operator fun get(index: kotlin.Int): E

    public abstract fun indexOf(element: E): kotlin.Int

    public abstract override fun isEmpty(): kotlin.Boolean

    public abstract override operator fun iterator(): kotlin.collections.Iterator<E>

    public abstract fun lastIndexOf(element: E): kotlin.Int

    public abstract fun listIterator(): kotlin.collections.ListIterator<E>

    public abstract fun listIterator(index: kotlin.Int): kotlin.collections.ListIterator<E>

    public abstract fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.List<E>
}

public interface ListIterator<out T> : kotlin.collections.Iterator<T> {
    public abstract override operator fun hasNext(): kotlin.Boolean

    public abstract fun hasPrevious(): kotlin.Boolean

    public abstract override operator fun next(): T

    public abstract fun nextIndex(): kotlin.Int

    public abstract fun previous(): T

    public abstract fun previousIndex(): kotlin.Int
}

public abstract class LongIterator : kotlin.collections.Iterator<kotlin.Long> {
    public constructor LongIterator()

    public final override operator fun next(): kotlin.Long

    public abstract fun nextLong(): kotlin.Long
}

public interface Map<K, out V> {
    public abstract val entries: kotlin.collections.Set<kotlin.collections.Map.Entry<K, V>> { get; }

    public abstract val keys: kotlin.collections.Set<K> { get; }

    public abstract val size: kotlin.Int { get; }

    public abstract val values: kotlin.collections.Collection<V> { get; }

    public abstract fun containsKey(key: K): kotlin.Boolean

    public abstract fun containsValue(value: V): kotlin.Boolean

    public abstract operator fun get(key: K): V?

    public abstract fun isEmpty(): kotlin.Boolean

    public interface Entry<out K, out V> {
        public abstract val key: K { get; }

        public abstract val value: V { get; }
    }
}

public interface MutableCollection<E> : kotlin.collections.Collection<E>, kotlin.collections.MutableIterable<E> {
    public abstract fun add(element: E): kotlin.Boolean

    public abstract fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun clear(): kotlin.Unit

    public abstract override operator fun iterator(): kotlin.collections.MutableIterator<E>

    public abstract fun remove(element: E): kotlin.Boolean

    public abstract fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public interface MutableIterable<out T> : kotlin.collections.Iterable<T> {
    public abstract override operator fun iterator(): kotlin.collections.MutableIterator<T>
}

public interface MutableIterator<out T> : kotlin.collections.Iterator<T> {
    public abstract fun remove(): kotlin.Unit
}

public interface MutableList<E> : kotlin.collections.List<E>, kotlin.collections.MutableCollection<E> {
    public abstract override fun add(element: E): kotlin.Boolean

    public abstract fun add(index: kotlin.Int, element: E): kotlin.Unit

    public abstract fun addAll(index: kotlin.Int, elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun clear(): kotlin.Unit

    public abstract override fun listIterator(): kotlin.collections.MutableListIterator<E>

    public abstract override fun listIterator(index: kotlin.Int): kotlin.collections.MutableListIterator<E>

    public abstract override fun remove(element: E): kotlin.Boolean

    public abstract override fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun removeAt(index: kotlin.Int): E

    public abstract override fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract operator fun set(index: kotlin.Int, element: E): E

    public abstract override fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.MutableList<E>
}

public interface MutableListIterator<T> : kotlin.collections.ListIterator<T>, kotlin.collections.MutableIterator<T> {
    public abstract fun add(element: T): kotlin.Unit

    public abstract override operator fun hasNext(): kotlin.Boolean

    public abstract override operator fun next(): T

    public abstract override fun remove(): kotlin.Unit

    public abstract fun set(element: T): kotlin.Unit
}

public interface MutableMap<K, V> : kotlin.collections.Map<K, V> {
    public abstract override val entries: kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>> { get; }

    public abstract override val keys: kotlin.collections.MutableSet<K> { get; }

    public abstract override val values: kotlin.collections.MutableCollection<V> { get; }

    public abstract fun clear(): kotlin.Unit

    public abstract fun put(key: K, value: V): V?

    public abstract fun putAll(from: kotlin.collections.Map<out K, V>): kotlin.Unit

    public abstract fun remove(key: K): V?

    public interface MutableEntry<K, V> : kotlin.collections.Map.Entry<K, V> {
        public abstract fun setValue(newValue: V): V
    }
}

public interface MutableSet<E> : kotlin.collections.Set<E>, kotlin.collections.MutableCollection<E> {
    public abstract override fun add(element: E): kotlin.Boolean

    public abstract override fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun clear(): kotlin.Unit

    public abstract override operator fun iterator(): kotlin.collections.MutableIterator<E>

    public abstract override fun remove(element: E): kotlin.Boolean

    public abstract override fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public interface RandomAccess {
}

public interface Set<out E> : kotlin.collections.Collection<E> {
    public abstract override val size: kotlin.Int { get; }

    public abstract override operator fun contains(element: E): kotlin.Boolean

    public abstract override fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract override fun isEmpty(): kotlin.Boolean

    public abstract override operator fun iterator(): kotlin.collections.Iterator<E>
}

public abstract class ShortIterator : kotlin.collections.Iterator<kotlin.Short> {
    public constructor ShortIterator()

    public final override operator fun next(): kotlin.Short

    public abstract fun nextShort(): kotlin.Short
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public abstract class UByteIterator : kotlin.collections.Iterator<kotlin.UByte> {
    public constructor UByteIterator()

    public final override operator fun next(): kotlin.UByte

    public abstract fun nextUByte(): kotlin.UByte
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public abstract class UIntIterator : kotlin.collections.Iterator<kotlin.UInt> {
    public constructor UIntIterator()

    public final override operator fun next(): kotlin.UInt

    public abstract fun nextUInt(): kotlin.UInt
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public abstract class ULongIterator : kotlin.collections.Iterator<kotlin.ULong> {
    public constructor ULongIterator()

    public final override operator fun next(): kotlin.ULong

    public abstract fun nextULong(): kotlin.ULong
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public abstract class UShortIterator : kotlin.collections.Iterator<kotlin.UShort> {
    public constructor UShortIterator()

    public final override operator fun next(): kotlin.UShort

    public abstract fun nextUShort(): kotlin.UShort
}