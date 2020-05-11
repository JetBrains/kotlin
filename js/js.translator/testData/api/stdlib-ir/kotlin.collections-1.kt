@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.forEach(/*0*/ action: (kotlin.UInt) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.forEach(/*0*/ action: (kotlin.ULong) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.forEach(/*0*/ action: (kotlin.UShort) -> kotlin.Unit): kotlin.Unit
@kotlin.internal.HidesMembers public inline fun </*0*/ T> kotlin.collections.Iterable<T>.forEach(/*0*/ action: (T) -> kotlin.Unit): kotlin.Unit
public inline fun </*0*/ T> kotlin.collections.Iterator<T>.forEach(/*0*/ operation: (T) -> kotlin.Unit): kotlin.Unit
@kotlin.internal.HidesMembers public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.forEach(/*0*/ action: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): kotlin.Unit
public inline fun </*0*/ T> kotlin.Array<out T>.forEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.BooleanArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.ByteArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Byte) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.CharArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.DoubleArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Double) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.FloatArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Float) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.IntArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Int) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.LongArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Long) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.ShortArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Short) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UByte) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UInt) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.ULong) -> kotlin.Unit): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UShort) -> kotlin.Unit): kotlin.Unit
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.forEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ @kotlin.internal.OnlyInputTypes K, /*1*/ V> kotlin.collections.Map<out K, V>.get(/*0*/ key: K): V?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Byte): kotlin.Byte
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Double): kotlin.Double
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Float): kotlin.Float
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Int): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Long): kotlin.Long
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.UShort): kotlin.UShort
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<K, V>.getOrElse(/*0*/ key: K, /*1*/ defaultValue: () -> V): V
public fun </*0*/ T> kotlin.Array<out T>.getOrNull(/*0*/ index: kotlin.Int): T?
public fun kotlin.BooleanArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Boolean?
public fun kotlin.ByteArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Byte?
public fun kotlin.CharArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Char?
public fun kotlin.DoubleArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Double?
public fun kotlin.FloatArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Float?
public fun kotlin.IntArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Int?
public fun kotlin.LongArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Long?
public fun kotlin.ShortArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.getOrNull(/*0*/ index: kotlin.Int): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.List<T>.getOrNull(/*0*/ index: kotlin.Int): T?
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.getOrPut(/*0*/ key: K, /*1*/ defaultValue: () -> V): V
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ K, /*1*/ V> kotlin.collections.Map<K, V>.getValue(/*0*/ key: K): V
@kotlin.internal.InlineOnly public inline operator fun </*0*/ V, /*1*/ V1 : V> kotlin.collections.Map<in kotlin.String, V>.getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): V1
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use getValue() with two type parameters instead") @kotlin.jvm.JvmName(name = "getVarContravariant") @kotlin.internal.LowPriorityInOverloadResolution @kotlin.internal.InlineOnly public inline fun </*0*/ V> kotlin.collections.MutableMap<in kotlin.String, in V>.getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): V
@kotlin.jvm.JvmName(name = "getVar") @kotlin.internal.InlineOnly public inline operator fun </*0*/ V, /*1*/ V1 : V> kotlin.collections.MutableMap<in kotlin.String, out V>.getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): V1
public inline fun </*0*/ T, /*1*/ K> kotlin.Array<out T>.groupBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.Array<out T>.groupBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.BooleanArray.groupBy(/*0*/ keySelector: (kotlin.Boolean) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Boolean>>
public inline fun </*0*/ K, /*1*/ V> kotlin.BooleanArray.groupBy(/*0*/ keySelector: (kotlin.Boolean) -> K, /*1*/ valueTransform: (kotlin.Boolean) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.ByteArray.groupBy(/*0*/ keySelector: (kotlin.Byte) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Byte>>
public inline fun </*0*/ K, /*1*/ V> kotlin.ByteArray.groupBy(/*0*/ keySelector: (kotlin.Byte) -> K, /*1*/ valueTransform: (kotlin.Byte) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.CharArray.groupBy(/*0*/ keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Char>>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharArray.groupBy(/*0*/ keySelector: (kotlin.Char) -> K, /*1*/ valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.DoubleArray.groupBy(/*0*/ keySelector: (kotlin.Double) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Double>>
public inline fun </*0*/ K, /*1*/ V> kotlin.DoubleArray.groupBy(/*0*/ keySelector: (kotlin.Double) -> K, /*1*/ valueTransform: (kotlin.Double) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.FloatArray.groupBy(/*0*/ keySelector: (kotlin.Float) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Float>>
public inline fun </*0*/ K, /*1*/ V> kotlin.FloatArray.groupBy(/*0*/ keySelector: (kotlin.Float) -> K, /*1*/ valueTransform: (kotlin.Float) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.IntArray.groupBy(/*0*/ keySelector: (kotlin.Int) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Int>>
public inline fun </*0*/ K, /*1*/ V> kotlin.IntArray.groupBy(/*0*/ keySelector: (kotlin.Int) -> K, /*1*/ valueTransform: (kotlin.Int) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.LongArray.groupBy(/*0*/ keySelector: (kotlin.Long) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Long>>
public inline fun </*0*/ K, /*1*/ V> kotlin.LongArray.groupBy(/*0*/ keySelector: (kotlin.Long) -> K, /*1*/ valueTransform: (kotlin.Long) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K> kotlin.ShortArray.groupBy(/*0*/ keySelector: (kotlin.Short) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Short>>
public inline fun </*0*/ K, /*1*/ V> kotlin.ShortArray.groupBy(/*0*/ keySelector: (kotlin.Short) -> K, /*1*/ valueTransform: (kotlin.Short) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K> kotlin.UByteArray.groupBy(/*0*/ keySelector: (kotlin.UByte) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UByte>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.UByteArray.groupBy(/*0*/ keySelector: (kotlin.UByte) -> K, /*1*/ valueTransform: (kotlin.UByte) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K> kotlin.UIntArray.groupBy(/*0*/ keySelector: (kotlin.UInt) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UInt>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.UIntArray.groupBy(/*0*/ keySelector: (kotlin.UInt) -> K, /*1*/ valueTransform: (kotlin.UInt) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K> kotlin.ULongArray.groupBy(/*0*/ keySelector: (kotlin.ULong) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.ULong>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.ULongArray.groupBy(/*0*/ keySelector: (kotlin.ULong) -> K, /*1*/ valueTransform: (kotlin.ULong) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K> kotlin.UShortArray.groupBy(/*0*/ keySelector: (kotlin.UShort) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.UShort>>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.UShortArray.groupBy(/*0*/ keySelector: (kotlin.UShort) -> K, /*1*/ valueTransform: (kotlin.UShort) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ T, /*1*/ K> kotlin.collections.Iterable<T>.groupBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.collections.Iterable<T>.groupBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.Array<out T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.Array<out T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Boolean>>> kotlin.BooleanArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Boolean) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.BooleanArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Boolean) -> K, /*2*/ valueTransform: (kotlin.Boolean) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Byte>>> kotlin.ByteArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Byte) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ByteArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Byte) -> K, /*2*/ valueTransform: (kotlin.Byte) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Char>>> kotlin.CharArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.CharArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K, /*2*/ valueTransform: (kotlin.Char) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Double>>> kotlin.DoubleArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Double) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.DoubleArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Double) -> K, /*2*/ valueTransform: (kotlin.Double) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Float>>> kotlin.FloatArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Float) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.FloatArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Float) -> K, /*2*/ valueTransform: (kotlin.Float) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Int>>> kotlin.IntArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Int) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.IntArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Int) -> K, /*2*/ valueTransform: (kotlin.Int) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Long>>> kotlin.LongArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Long) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.LongArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Long) -> K, /*2*/ valueTransform: (kotlin.Long) -> V): M
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Short>>> kotlin.ShortArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Short) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ShortArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Short) -> K, /*2*/ valueTransform: (kotlin.Short) -> V): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UByte>>> kotlin.UByteArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UByte) -> K): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UByteArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UByte) -> K, /*2*/ valueTransform: (kotlin.UByte) -> V): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UInt>>> kotlin.UIntArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UInt) -> K): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UIntArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UInt) -> K, /*2*/ valueTransform: (kotlin.UInt) -> V): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.ULong>>> kotlin.ULongArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.ULong) -> K): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.ULongArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.ULong) -> K, /*2*/ valueTransform: (kotlin.ULong) -> V): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.UShort>>> kotlin.UShortArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UShort) -> K): M
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.UShortArray.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.UShort) -> K, /*2*/ valueTransform: (kotlin.UShort) -> V): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.collections.Iterable<T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.collections.Iterable<T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K> kotlin.Array<out T>.groupingBy(/*0*/ crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K> kotlin.collections.Iterable<T>.groupingBy(/*0*/ crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.Array<*>, /*1*/ R> C.ifEmpty(/*0*/ defaultValue: () -> R): R where C : R
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.collections.Collection<*>, /*1*/ R> C.ifEmpty(/*0*/ defaultValue: () -> R): R where C : R
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ M : kotlin.collections.Map<*, *>, /*1*/ R> M.ifEmpty(/*0*/ defaultValue: () -> R): R where M : R
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.Array<out T>.indexOf(/*0*/ element: T): kotlin.Int
public fun kotlin.BooleanArray.indexOf(/*0*/ element: kotlin.Boolean): kotlin.Int
public fun kotlin.ByteArray.indexOf(/*0*/ element: kotlin.Byte): kotlin.Int
public fun kotlin.CharArray.indexOf(/*0*/ element: kotlin.Char): kotlin.Int
public fun kotlin.DoubleArray.indexOf(/*0*/ element: kotlin.Double): kotlin.Int
public fun kotlin.FloatArray.indexOf(/*0*/ element: kotlin.Float): kotlin.Int
public fun kotlin.IntArray.indexOf(/*0*/ element: kotlin.Int): kotlin.Int
public fun kotlin.LongArray.indexOf(/*0*/ element: kotlin.Long): kotlin.Int
public fun kotlin.ShortArray.indexOf(/*0*/ element: kotlin.Short): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.indexOf(/*0*/ element: kotlin.UByte): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.indexOf(/*0*/ element: kotlin.UInt): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.indexOf(/*0*/ element: kotlin.ULong): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.indexOf(/*0*/ element: kotlin.UShort): kotlin.Int
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.Iterable<T>.indexOf(/*0*/ element: T): kotlin.Int
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.List<T>.indexOf(/*0*/ element: T): kotlin.Int
public inline fun </*0*/ T> kotlin.Array<out T>.indexOfFirst(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.BooleanArray.indexOfFirst(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.ByteArray.indexOfFirst(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.CharArray.indexOfFirst(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.DoubleArray.indexOfFirst(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.FloatArray.indexOfFirst(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.IntArray.indexOfFirst(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.LongArray.indexOfFirst(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.ShortArray.indexOfFirst(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.indexOfFirst(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.indexOfFirst(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.indexOfFirst(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.indexOfFirst(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.indexOfFirst(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.collections.List<T>.indexOfFirst(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.Array<out T>.indexOfLast(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.BooleanArray.indexOfLast(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.ByteArray.indexOfLast(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.CharArray.indexOfLast(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.DoubleArray.indexOfLast(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.FloatArray.indexOfLast(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.IntArray.indexOfLast(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.LongArray.indexOfLast(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.ShortArray.indexOfLast(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.indexOfLast(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.indexOfLast(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.indexOfLast(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.indexOfLast(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.indexOfLast(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.collections.List<T>.indexOfLast(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public infix fun </*0*/ T> kotlin.Array<out T>.intersect(/*0*/ other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>
public infix fun kotlin.BooleanArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Boolean>): kotlin.collections.Set<kotlin.Boolean>
public infix fun kotlin.ByteArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Byte>): kotlin.collections.Set<kotlin.Byte>
public infix fun kotlin.CharArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Char>): kotlin.collections.Set<kotlin.Char>
public infix fun kotlin.DoubleArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Double>): kotlin.collections.Set<kotlin.Double>
public infix fun kotlin.FloatArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Float>): kotlin.collections.Set<kotlin.Float>
public infix fun kotlin.IntArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Int>): kotlin.collections.Set<kotlin.Int>
public infix fun kotlin.LongArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Long>): kotlin.collections.Set<kotlin.Long>
public infix fun kotlin.ShortArray.intersect(/*0*/ other: kotlin.collections.Iterable<kotlin.Short>): kotlin.collections.Set<kotlin.Short>
public infix fun </*0*/ T> kotlin.collections.Iterable<T>.intersect(/*0*/ other: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.isEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.IntArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.LongArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.isNotEmpty(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.Array<*>?.isNullOrEmpty(): kotlin.Boolean
    Returns(FALSE) -> <this> != null

@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>?.isNullOrEmpty(): kotlin.Boolean
    Returns(FALSE) -> <this> != null

@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>?.isNullOrEmpty(): kotlin.Boolean
    Returns(FALSE) -> <this> != null

@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.Iterator<T>.iterator(): kotlin.collections.Iterator<T>
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.iterator(): kotlin.collections.Iterator<kotlin.collections.Map.Entry<K, V>>
@kotlin.jvm.JvmName(name = "mutableIterator") @kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.iterator(): kotlin.collections.MutableIterator<kotlin.collections.MutableMap.MutableEntry<K, V>>
public fun </*0*/ T, /*1*/ A : kotlin.text.Appendable> kotlin.Array<out T>.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((T) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.BooleanArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Boolean) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.ByteArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Byte) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.CharArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Char) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.DoubleArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Double) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.FloatArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Float) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.IntArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Int) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.LongArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Long) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ A : kotlin.text.Appendable> kotlin.ShortArray.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((kotlin.Short) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ T, /*1*/ A : kotlin.text.Appendable> kotlin.collections.Iterable<T>.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((T) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ T> kotlin.Array<out T>.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.BooleanArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Boolean) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.ByteArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Byte) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.CharArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Char) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.DoubleArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Double) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.FloatArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Float) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.IntArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Int) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.LongArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Long) -> kotlin.CharSequence)? = ...): kotlin.String
public fun kotlin.ShortArray.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((kotlin.Short) -> kotlin.CharSequence)? = ...): kotlin.String
public fun </*0*/ T> kotlin.collections.Iterable<T>.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String
public fun </*0*/ T> kotlin.Array<out T>.last(): T
public inline fun </*0*/ T> kotlin.Array<out T>.last(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun kotlin.BooleanArray.last(): kotlin.Boolean
public inline fun kotlin.BooleanArray.last(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ByteArray.last(): kotlin.Byte
public inline fun kotlin.ByteArray.last(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte
public fun kotlin.CharArray.last(): kotlin.Char
public inline fun kotlin.CharArray.last(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char
public fun kotlin.DoubleArray.last(): kotlin.Double
public inline fun kotlin.DoubleArray.last(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double
public fun kotlin.FloatArray.last(): kotlin.Float
public inline fun kotlin.FloatArray.last(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float
public fun kotlin.IntArray.last(): kotlin.Int
public inline fun kotlin.IntArray.last(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int
public fun kotlin.LongArray.last(): kotlin.Long
public inline fun kotlin.LongArray.last(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long
public fun kotlin.ShortArray.last(): kotlin.Short
public inline fun kotlin.ShortArray.last(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.last(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.last(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.last(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.last(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.last(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.last(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.last(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.last(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort
public fun </*0*/ T> kotlin.collections.Iterable<T>.last(): T
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.last(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ T> kotlin.collections.List<T>.last(): T
public inline fun </*0*/ T> kotlin.collections.List<T>.last(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.Array<out T>.lastIndexOf(/*0*/ element: T): kotlin.Int
public fun kotlin.BooleanArray.lastIndexOf(/*0*/ element: kotlin.Boolean): kotlin.Int
public fun kotlin.ByteArray.lastIndexOf(/*0*/ element: kotlin.Byte): kotlin.Int
public fun kotlin.CharArray.lastIndexOf(/*0*/ element: kotlin.Char): kotlin.Int
public fun kotlin.DoubleArray.lastIndexOf(/*0*/ element: kotlin.Double): kotlin.Int
public fun kotlin.FloatArray.lastIndexOf(/*0*/ element: kotlin.Float): kotlin.Int
public fun kotlin.IntArray.lastIndexOf(/*0*/ element: kotlin.Int): kotlin.Int
public fun kotlin.LongArray.lastIndexOf(/*0*/ element: kotlin.Long): kotlin.Int
public fun kotlin.ShortArray.lastIndexOf(/*0*/ element: kotlin.Short): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.lastIndexOf(/*0*/ element: kotlin.UByte): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.lastIndexOf(/*0*/ element: kotlin.UInt): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.lastIndexOf(/*0*/ element: kotlin.ULong): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.lastIndexOf(/*0*/ element: kotlin.UShort): kotlin.Int
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.Iterable<T>.lastIndexOf(/*0*/ element: T): kotlin.Int
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.collections.List<T>.lastIndexOf(/*0*/ element: T): kotlin.Int
public fun </*0*/ T> kotlin.Array<out T>.lastOrNull(): T?
public inline fun </*0*/ T> kotlin.Array<out T>.lastOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun kotlin.BooleanArray.lastOrNull(): kotlin.Boolean?
public inline fun kotlin.BooleanArray.lastOrNull(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
public fun kotlin.ByteArray.lastOrNull(): kotlin.Byte?
public inline fun kotlin.ByteArray.lastOrNull(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Byte?
public fun kotlin.CharArray.lastOrNull(): kotlin.Char?
public inline fun kotlin.CharArray.lastOrNull(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.DoubleArray.lastOrNull(): kotlin.Double?
public inline fun kotlin.DoubleArray.lastOrNull(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Double?
public fun kotlin.FloatArray.lastOrNull(): kotlin.Float?
public inline fun kotlin.FloatArray.lastOrNull(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Float?
public fun kotlin.IntArray.lastOrNull(): kotlin.Int?
public inline fun kotlin.IntArray.lastOrNull(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Int?
public fun kotlin.LongArray.lastOrNull(): kotlin.Long?
public inline fun kotlin.LongArray.lastOrNull(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Long?
public fun kotlin.ShortArray.lastOrNull(): kotlin.Short?
public inline fun kotlin.ShortArray.lastOrNull(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.lastOrNull(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.lastOrNull(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.lastOrNull(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.lastOrNull(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.lastOrNull(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.lastOrNull(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.lastOrNull(): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.lastOrNull(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.Iterable<T>.lastOrNull(): T?
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.lastOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T> kotlin.collections.List<T>.lastOrNull(): T?
public inline fun </*0*/ T> kotlin.collections.List<T>.lastOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.map(/*0*/ transform: (T) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.BooleanArray.map(/*0*/ transform: (kotlin.Boolean) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ByteArray.map(/*0*/ transform: (kotlin.Byte) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.CharArray.map(/*0*/ transform: (kotlin.Char) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.DoubleArray.map(/*0*/ transform: (kotlin.Double) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.FloatArray.map(/*0*/ transform: (kotlin.Float) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.IntArray.map(/*0*/ transform: (kotlin.Int) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.LongArray.map(/*0*/ transform: (kotlin.Long) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ShortArray.map(/*0*/ transform: (kotlin.Short) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.map(/*0*/ transform: (kotlin.UByte) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.map(/*0*/ transform: (kotlin.UInt) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.map(/*0*/ transform: (kotlin.ULong) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.map(/*0*/ transform: (kotlin.UShort) -> R): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.map(/*0*/ transform: (T) -> R): kotlin.collections.List<R>
public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.map(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.mapIndexed(/*0*/ transform: (index: kotlin.Int, T) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.BooleanArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Boolean) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ByteArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Byte) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.CharArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Char) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.DoubleArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Double) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.FloatArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Float) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.IntArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Int) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.LongArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Long) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.ShortArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Short) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.UByte) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.UInt) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.ULong) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.UShort) -> R): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.mapIndexed(/*0*/ transform: (index: kotlin.Int, T) -> R): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.Array<out T>.mapIndexedNotNull(/*0*/ transform: (index: kotlin.Int, T) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.collections.Iterable<T>.mapIndexedNotNull(/*0*/ transform: (index: kotlin.Int, T) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapIndexedNotNullTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R?): C
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapIndexedNotNullTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R?): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Boolean) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Byte) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Char) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Double) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Float) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Int) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Long) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Short) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.UByte) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.UInt) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.ULong) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.UShort) -> R): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.mapKeys(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map<R, V>
public inline fun </*0*/ K, /*1*/ V, /*2*/ R, /*3*/ M : kotlin.collections.MutableMap<in R, in V>> kotlin.collections.Map<out K, V>.mapKeysTo(/*0*/ destination: M, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): M
public inline fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.Array<out T>.mapNotNull(/*0*/ transform: (T) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.collections.Iterable<T>.mapNotNull(/*0*/ transform: (T) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Any> kotlin.collections.Map<out K, V>.mapNotNull(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapNotNullTo(/*0*/ destination: C, /*1*/ transform: (T) -> R?): C
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapNotNullTo(/*0*/ destination: C, /*1*/ transform: (T) -> R?): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Any, /*3*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.mapNotNullTo(/*0*/ destination: C, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R?): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.Array<out T>.mapTo(/*0*/ destination: C, /*1*/ transform: (T) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.BooleanArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Boolean) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ByteArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Byte) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Char) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.DoubleArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Double) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.FloatArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Float) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.IntArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Int) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.LongArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Long) -> R): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ShortArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Short) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UByteArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UByte) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UIntArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UInt) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.ULongArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.ULong) -> R): C
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.UShortArray.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.UShort) -> R): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Iterable<T>.mapTo(/*0*/ destination: C, /*1*/ transform: (T) -> R): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ R, /*3*/ C : kotlin.collections.MutableCollection<in R>> kotlin.collections.Map<out K, V>.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): C
public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.mapValues(/*0*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map<K, R>
public inline fun </*0*/ K, /*1*/ V, /*2*/ R, /*3*/ M : kotlin.collections.MutableMap<in K, in R>> kotlin.collections.Map<out K, V>.mapValuesTo(/*0*/ destination: M, /*1*/ transform: (kotlin.collections.Map.Entry<K, V>) -> R): M
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.Array<out T>.max(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.Array<out kotlin.Double>.max(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.Array<out kotlin.Float>.max(): kotlin.Float?
public fun kotlin.ByteArray.max(): kotlin.Byte?
public fun kotlin.CharArray.max(): kotlin.Char?
public fun kotlin.DoubleArray.max(): kotlin.Double?
public fun kotlin.FloatArray.max(): kotlin.Float?
public fun kotlin.IntArray.max(): kotlin.Int?
public fun kotlin.LongArray.max(): kotlin.Long?
public fun kotlin.ShortArray.max(): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.max(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.max(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.max(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.max(): kotlin.UShort?
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.max(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.collections.Iterable<kotlin.Double>.max(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.collections.Iterable<kotlin.Float>.max(): kotlin.Float?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.maxBy(/*0*/ selector: (T) -> R): T?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.maxBy(/*0*/ selector: (kotlin.Boolean) -> R): kotlin.Boolean?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.maxBy(/*0*/ selector: (kotlin.Byte) -> R): kotlin.Byte?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.maxBy(/*0*/ selector: (kotlin.Char) -> R): kotlin.Char?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.maxBy(/*0*/ selector: (kotlin.Double) -> R): kotlin.Double?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.maxBy(/*0*/ selector: (kotlin.Float) -> R): kotlin.Float?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.maxBy(/*0*/ selector: (kotlin.Int) -> R): kotlin.Int?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.maxBy(/*0*/ selector: (kotlin.Long) -> R): kotlin.Long?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.maxBy(/*0*/ selector: (kotlin.Short) -> R): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.maxBy(/*0*/ selector: (kotlin.UByte) -> R): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.maxBy(/*0*/ selector: (kotlin.UInt) -> R): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.maxBy(/*0*/ selector: (kotlin.ULong) -> R): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.maxBy(/*0*/ selector: (kotlin.UShort) -> R): kotlin.UShort?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxBy(/*0*/ selector: (T) -> R): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxBy(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.maxOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.maxOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.maxOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.maxOf(/*0*/ selector: (kotlin.Boolean) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.maxOf(/*0*/ selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.maxOf(/*0*/ selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.maxOf(/*0*/ selector: (kotlin.Byte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.maxOf(/*0*/ selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.maxOf(/*0*/ selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.maxOf(/*0*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.maxOf(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.maxOf(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.maxOf(/*0*/ selector: (kotlin.Double) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.maxOf(/*0*/ selector: (kotlin.Double) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.maxOf(/*0*/ selector: (kotlin.Double) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.maxOf(/*0*/ selector: (kotlin.Float) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.maxOf(/*0*/ selector: (kotlin.Float) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.maxOf(/*0*/ selector: (kotlin.Float) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.maxOf(/*0*/ selector: (kotlin.Int) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.maxOf(/*0*/ selector: (kotlin.Int) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.maxOf(/*0*/ selector: (kotlin.Int) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.maxOf(/*0*/ selector: (kotlin.Long) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.maxOf(/*0*/ selector: (kotlin.Long) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.maxOf(/*0*/ selector: (kotlin.Long) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.maxOf(/*0*/ selector: (kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.maxOf(/*0*/ selector: (kotlin.Short) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.maxOf(/*0*/ selector: (kotlin.Short) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.maxOf(/*0*/ selector: (kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.maxOf(/*0*/ selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.maxOf(/*0*/ selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.maxOf(/*0*/ selector: (kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.maxOf(/*0*/ selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.maxOf(/*0*/ selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.maxOf(/*0*/ selector: (kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.maxOf(/*0*/ selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.maxOf(/*0*/ selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.maxOf(/*0*/ selector: (kotlin.UShort) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.maxOf(/*0*/ selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.maxOf(/*0*/ selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.maxOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.maxOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.maxOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.maxOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.maxOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.maxOfOrNull(/*0*/ selector: (kotlin.Boolean) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.maxOfOrNull(/*0*/ selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.maxOfOrNull(/*0*/ selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.maxOfOrNull(/*0*/ selector: (kotlin.Byte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.maxOfOrNull(/*0*/ selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.maxOfOrNull(/*0*/ selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.maxOfOrNull(/*0*/ selector: (kotlin.Double) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.maxOfOrNull(/*0*/ selector: (kotlin.Double) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.maxOfOrNull(/*0*/ selector: (kotlin.Double) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.maxOfOrNull(/*0*/ selector: (kotlin.Float) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.maxOfOrNull(/*0*/ selector: (kotlin.Float) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.maxOfOrNull(/*0*/ selector: (kotlin.Float) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.maxOfOrNull(/*0*/ selector: (kotlin.Int) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.maxOfOrNull(/*0*/ selector: (kotlin.Int) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.maxOfOrNull(/*0*/ selector: (kotlin.Int) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.maxOfOrNull(/*0*/ selector: (kotlin.Long) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.maxOfOrNull(/*0*/ selector: (kotlin.Long) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.maxOfOrNull(/*0*/ selector: (kotlin.Long) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.maxOfOrNull(/*0*/ selector: (kotlin.Short) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.maxOfOrNull(/*0*/ selector: (kotlin.Short) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.maxOfOrNull(/*0*/ selector: (kotlin.Short) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.maxOfOrNull(/*0*/ selector: (kotlin.UByte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.maxOfOrNull(/*0*/ selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.maxOfOrNull(/*0*/ selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.maxOfOrNull(/*0*/ selector: (kotlin.UInt) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.maxOfOrNull(/*0*/ selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.maxOfOrNull(/*0*/ selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.maxOfOrNull(/*0*/ selector: (kotlin.ULong) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.maxOfOrNull(/*0*/ selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.maxOfOrNull(/*0*/ selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.maxOfOrNull(/*0*/ selector: (kotlin.UShort) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.maxOfOrNull(/*0*/ selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.maxOfOrNull(/*0*/ selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.maxOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.maxOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.maxOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.maxOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.BooleanArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Boolean) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ByteArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Byte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.DoubleArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Double) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.FloatArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Float) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.IntArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Int) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.LongArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Long) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ShortArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UShort) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.BooleanArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Boolean) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ByteArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Byte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.DoubleArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Double) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.FloatArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Float) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.IntArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Int) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.LongArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Long) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ShortArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Short) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UByte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UInt) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.ULong) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UShort) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?
public fun </*0*/ T> kotlin.Array<out T>.maxWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
public fun kotlin.BooleanArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?
public fun kotlin.ByteArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?
public fun kotlin.CharArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?
public fun kotlin.DoubleArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?
public fun kotlin.FloatArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?
public fun kotlin.IntArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?
public fun kotlin.LongArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?
public fun kotlin.ShortArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.Iterable<T>.maxWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.Array<out T>.min(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.Array<out kotlin.Double>.min(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.Array<out kotlin.Float>.min(): kotlin.Float?
public fun kotlin.ByteArray.min(): kotlin.Byte?
public fun kotlin.CharArray.min(): kotlin.Char?
public fun kotlin.DoubleArray.min(): kotlin.Double?
public fun kotlin.FloatArray.min(): kotlin.Float?
public fun kotlin.IntArray.min(): kotlin.Int?
public fun kotlin.LongArray.min(): kotlin.Long?
public fun kotlin.ShortArray.min(): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.min(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.min(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.min(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.min(): kotlin.UShort?
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.collections.Iterable<T>.min(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.collections.Iterable<kotlin.Double>.min(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.collections.Iterable<kotlin.Float>.min(): kotlin.Float?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.minBy(/*0*/ selector: (T) -> R): T?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.minBy(/*0*/ selector: (kotlin.Boolean) -> R): kotlin.Boolean?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.minBy(/*0*/ selector: (kotlin.Byte) -> R): kotlin.Byte?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.minBy(/*0*/ selector: (kotlin.Char) -> R): kotlin.Char?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.minBy(/*0*/ selector: (kotlin.Double) -> R): kotlin.Double?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.minBy(/*0*/ selector: (kotlin.Float) -> R): kotlin.Float?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.minBy(/*0*/ selector: (kotlin.Int) -> R): kotlin.Int?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.minBy(/*0*/ selector: (kotlin.Long) -> R): kotlin.Long?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.minBy(/*0*/ selector: (kotlin.Short) -> R): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.minBy(/*0*/ selector: (kotlin.UByte) -> R): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.minBy(/*0*/ selector: (kotlin.UInt) -> R): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.minBy(/*0*/ selector: (kotlin.ULong) -> R): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.minBy(/*0*/ selector: (kotlin.UShort) -> R): kotlin.UShort?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minBy(/*0*/ selector: (T) -> R): T?
public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minBy(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): kotlin.collections.Map.Entry<K, V>?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.minOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.minOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.minOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.minOf(/*0*/ selector: (kotlin.Boolean) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.minOf(/*0*/ selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.minOf(/*0*/ selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.minOf(/*0*/ selector: (kotlin.Byte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.minOf(/*0*/ selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.minOf(/*0*/ selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.minOf(/*0*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.minOf(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.minOf(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.minOf(/*0*/ selector: (kotlin.Double) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.minOf(/*0*/ selector: (kotlin.Double) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.minOf(/*0*/ selector: (kotlin.Double) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.minOf(/*0*/ selector: (kotlin.Float) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.minOf(/*0*/ selector: (kotlin.Float) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.minOf(/*0*/ selector: (kotlin.Float) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.minOf(/*0*/ selector: (kotlin.Int) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.minOf(/*0*/ selector: (kotlin.Int) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.minOf(/*0*/ selector: (kotlin.Int) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.minOf(/*0*/ selector: (kotlin.Long) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.minOf(/*0*/ selector: (kotlin.Long) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.minOf(/*0*/ selector: (kotlin.Long) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.minOf(/*0*/ selector: (kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.minOf(/*0*/ selector: (kotlin.Short) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.minOf(/*0*/ selector: (kotlin.Short) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.minOf(/*0*/ selector: (kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.minOf(/*0*/ selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.minOf(/*0*/ selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.minOf(/*0*/ selector: (kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.minOf(/*0*/ selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.minOf(/*0*/ selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.minOf(/*0*/ selector: (kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.minOf(/*0*/ selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.minOf(/*0*/ selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.minOf(/*0*/ selector: (kotlin.UShort) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.minOf(/*0*/ selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.minOf(/*0*/ selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.minOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.minOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minOf(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.Array<out T>.minOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.BooleanArray.minOfOrNull(/*0*/ selector: (kotlin.Boolean) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.minOfOrNull(/*0*/ selector: (kotlin.Boolean) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.minOfOrNull(/*0*/ selector: (kotlin.Boolean) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ByteArray.minOfOrNull(/*0*/ selector: (kotlin.Byte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.minOfOrNull(/*0*/ selector: (kotlin.Byte) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.minOfOrNull(/*0*/ selector: (kotlin.Byte) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharArray.minOfOrNull(/*0*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.minOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.minOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.DoubleArray.minOfOrNull(/*0*/ selector: (kotlin.Double) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.minOfOrNull(/*0*/ selector: (kotlin.Double) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.minOfOrNull(/*0*/ selector: (kotlin.Double) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.FloatArray.minOfOrNull(/*0*/ selector: (kotlin.Float) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.minOfOrNull(/*0*/ selector: (kotlin.Float) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.minOfOrNull(/*0*/ selector: (kotlin.Float) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.IntArray.minOfOrNull(/*0*/ selector: (kotlin.Int) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.minOfOrNull(/*0*/ selector: (kotlin.Int) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.minOfOrNull(/*0*/ selector: (kotlin.Int) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.LongArray.minOfOrNull(/*0*/ selector: (kotlin.Long) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.minOfOrNull(/*0*/ selector: (kotlin.Long) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.minOfOrNull(/*0*/ selector: (kotlin.Long) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ShortArray.minOfOrNull(/*0*/ selector: (kotlin.Short) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.minOfOrNull(/*0*/ selector: (kotlin.Short) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.minOfOrNull(/*0*/ selector: (kotlin.Short) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UByteArray.minOfOrNull(/*0*/ selector: (kotlin.UByte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.minOfOrNull(/*0*/ selector: (kotlin.UByte) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.minOfOrNull(/*0*/ selector: (kotlin.UByte) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UIntArray.minOfOrNull(/*0*/ selector: (kotlin.UInt) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.minOfOrNull(/*0*/ selector: (kotlin.UInt) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.minOfOrNull(/*0*/ selector: (kotlin.UInt) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.ULongArray.minOfOrNull(/*0*/ selector: (kotlin.ULong) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.minOfOrNull(/*0*/ selector: (kotlin.ULong) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.minOfOrNull(/*0*/ selector: (kotlin.ULong) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.UShortArray.minOfOrNull(/*0*/ selector: (kotlin.UShort) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.minOfOrNull(/*0*/ selector: (kotlin.UShort) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.minOfOrNull(/*0*/ selector: (kotlin.UShort) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.collections.Iterable<T>.minOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R : kotlin.Comparable<R>> kotlin.collections.Map<out K, V>.minOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minOfOrNull(/*0*/ selector: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.BooleanArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Boolean) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ByteArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Byte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.DoubleArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Double) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.FloatArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Float) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.IntArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Int) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.LongArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Long) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ShortArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Short) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UByte) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UInt) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.ULong) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UShort) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.Array<out T>.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.BooleanArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Boolean) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ByteArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Byte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.DoubleArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Double) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.FloatArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Float) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.IntArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Int) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.LongArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Long) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ShortArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Short) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UByteArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UByte) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UIntArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UInt) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.ULongArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.ULong) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.UShortArray.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.UShort) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.collections.Iterable<T>.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V, /*2*/ R> kotlin.collections.Map<out K, V>.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.collections.Map.Entry<K, V>) -> R): R?
public fun </*0*/ T> kotlin.Array<out T>.minWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
public fun kotlin.BooleanArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Boolean>): kotlin.Boolean?
public fun kotlin.ByteArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Byte>): kotlin.Byte?
public fun kotlin.CharArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?
public fun kotlin.DoubleArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Double>): kotlin.Double?
public fun kotlin.FloatArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Float>): kotlin.Float?
public fun kotlin.IntArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Int>): kotlin.Int?
public fun kotlin.LongArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Long>): kotlin.Long?
public fun kotlin.ShortArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Short>): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UByte>): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UInt>): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.ULong>): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.UShort>): kotlin.UShort?
public fun </*0*/ T> kotlin.collections.Iterable<T>.minWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
public fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.collections.Map.Entry<K, V>>): kotlin.collections.Map.Entry<K, V>?
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.minus(/*0*/ element: T): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.minus(/*0*/ elements: kotlin.Array<out T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.minus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.minus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>
@kotlin.SinceKotlin(version = "1.1") public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minus(/*0*/ key: K): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.1") public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minus(/*0*/ keys: kotlin.Array<out K>): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.1") public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minus(/*0*/ keys: kotlin.collections.Iterable<K>): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.1") public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.minus(/*0*/ keys: kotlin.sequences.Sequence<K>): kotlin.collections.Map<K, V>
public operator fun </*0*/ T> kotlin.collections.Set<T>.minus(/*0*/ element: T): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.minus(/*0*/ elements: kotlin.Array<out T>): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.minus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.minus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.collections.Set<T>
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.minusAssign(/*0*/ element: T): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.minusAssign(/*0*/ elements: kotlin.Array<T>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.minusAssign(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.minusAssign(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.Unit
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.minusAssign(/*0*/ key: K): kotlin.Unit
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.minusAssign(/*0*/ keys: kotlin.Array<out K>): kotlin.Unit
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.minusAssign(/*0*/ keys: kotlin.collections.Iterable<K>): kotlin.Unit
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<K, V>.minusAssign(/*0*/ keys: kotlin.sequences.Sequence<K>): kotlin.Unit
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.minusElement(/*0*/ element: T): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Set<T>.minusElement(/*0*/ element: T): kotlin.collections.Set<T>
public fun </*0*/ T> kotlin.Array<out T>.none(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.Array<out T>.none(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.BooleanArray.none(): kotlin.Boolean
public inline fun kotlin.BooleanArray.none(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ByteArray.none(): kotlin.Boolean
public inline fun kotlin.ByteArray.none(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.CharArray.none(): kotlin.Boolean
public inline fun kotlin.CharArray.none(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.DoubleArray.none(): kotlin.Boolean
public inline fun kotlin.DoubleArray.none(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.FloatArray.none(): kotlin.Boolean
public inline fun kotlin.FloatArray.none(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.IntArray.none(): kotlin.Boolean
public inline fun kotlin.IntArray.none(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.LongArray.none(): kotlin.Boolean
public inline fun kotlin.LongArray.none(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.ShortArray.none(): kotlin.Boolean
public inline fun kotlin.ShortArray.none(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.none(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.none(/*0*/ predicate: (kotlin.UByte) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.none(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.none(/*0*/ predicate: (kotlin.UInt) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.none(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.none(/*0*/ predicate: (kotlin.ULong) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.none(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.none(/*0*/ predicate: (kotlin.UShort) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T> kotlin.collections.Iterable<T>.none(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.none(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.none(): kotlin.Boolean
public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.none(/*0*/ predicate: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ C : kotlin.collections.Iterable<T>> C.onEach(/*0*/ action: (T) -> kotlin.Unit): C
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.Map<out K, V>> M.onEach(/*0*/ action: (kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.onEach(/*0*/ action: (T) -> kotlin.Unit): kotlin.Array<out T>
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.onEach(/*0*/ action: (kotlin.Boolean) -> kotlin.Unit): kotlin.BooleanArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.onEach(/*0*/ action: (kotlin.Byte) -> kotlin.Unit): kotlin.ByteArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.onEach(/*0*/ action: (kotlin.Char) -> kotlin.Unit): kotlin.CharArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.onEach(/*0*/ action: (kotlin.Double) -> kotlin.Unit): kotlin.DoubleArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.onEach(/*0*/ action: (kotlin.Float) -> kotlin.Unit): kotlin.FloatArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.onEach(/*0*/ action: (kotlin.Int) -> kotlin.Unit): kotlin.IntArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.onEach(/*0*/ action: (kotlin.Long) -> kotlin.Unit): kotlin.LongArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.onEach(/*0*/ action: (kotlin.Short) -> kotlin.Unit): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.onEach(/*0*/ action: (kotlin.UByte) -> kotlin.Unit): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.onEach(/*0*/ action: (kotlin.UInt) -> kotlin.Unit): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.onEach(/*0*/ action: (kotlin.ULong) -> kotlin.Unit): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.onEach(/*0*/ action: (kotlin.UShort) -> kotlin.Unit): kotlin.UShortArray
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ T, /*1*/ C : kotlin.collections.Iterable<T>> C.onEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): C
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.Map<out K, V>> M.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.collections.Map.Entry<K, V>) -> kotlin.Unit): M
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.onEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Array<out T>
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Boolean) -> kotlin.Unit): kotlin.BooleanArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Byte) -> kotlin.Unit): kotlin.ByteArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.CharArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Double) -> kotlin.Unit): kotlin.DoubleArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Float) -> kotlin.Unit): kotlin.FloatArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Int) -> kotlin.Unit): kotlin.IntArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Long) -> kotlin.Unit): kotlin.LongArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Short) -> kotlin.Unit): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UByte) -> kotlin.Unit): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UInt) -> kotlin.Unit): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.ULong) -> kotlin.Unit): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.UShort) -> kotlin.Unit): kotlin.UShortArray
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>?.orEmpty(): kotlin.Array<out T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>?.orEmpty(): kotlin.collections.Collection<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.List<T>?.orEmpty(): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ K, /*1*/ V> kotlin.collections.Map<K, V>?.orEmpty(): kotlin.collections.Map<K, V>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Set<T>?.orEmpty(): kotlin.collections.Set<T>
public inline fun </*0*/ T> kotlin.Array<out T>.partition(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>
public inline fun kotlin.BooleanArray.partition(/*0*/ predicate: (kotlin.Boolean) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Boolean>, kotlin.collections.List<kotlin.Boolean>>
public inline fun kotlin.ByteArray.partition(/*0*/ predicate: (kotlin.Byte) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Byte>, kotlin.collections.List<kotlin.Byte>>
public inline fun kotlin.CharArray.partition(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Char>, kotlin.collections.List<kotlin.Char>>
public inline fun kotlin.DoubleArray.partition(/*0*/ predicate: (kotlin.Double) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Double>, kotlin.collections.List<kotlin.Double>>
public inline fun kotlin.FloatArray.partition(/*0*/ predicate: (kotlin.Float) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Float>, kotlin.collections.List<kotlin.Float>>
public inline fun kotlin.IntArray.partition(/*0*/ predicate: (kotlin.Int) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Int>, kotlin.collections.List<kotlin.Int>>
public inline fun kotlin.LongArray.partition(/*0*/ predicate: (kotlin.Long) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Long>, kotlin.collections.List<kotlin.Long>>
public inline fun kotlin.ShortArray.partition(/*0*/ predicate: (kotlin.Short) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<kotlin.Short>, kotlin.collections.List<kotlin.Short>>
public inline fun </*0*/ T> kotlin.collections.Iterable<T>.partition(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>
public inline operator fun </*0*/ T> kotlin.Array<out T>.plus(/*0*/ element: T): kotlin.Array<T>
public inline operator fun </*0*/ T> kotlin.Array<out T>.plus(/*0*/ elements: kotlin.Array<out T>): kotlin.Array<T>
public operator fun </*0*/ T> kotlin.Array<out T>.plus(/*0*/ elements: kotlin.collections.Collection<T>): kotlin.Array<T>
public inline operator fun kotlin.BooleanArray.plus(/*0*/ element: kotlin.Boolean): kotlin.BooleanArray
public inline operator fun kotlin.BooleanArray.plus(/*0*/ elements: kotlin.BooleanArray): kotlin.BooleanArray
public operator fun kotlin.BooleanArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Boolean>): kotlin.BooleanArray
public inline operator fun kotlin.ByteArray.plus(/*0*/ element: kotlin.Byte): kotlin.ByteArray
public inline operator fun kotlin.ByteArray.plus(/*0*/ elements: kotlin.ByteArray): kotlin.ByteArray
public operator fun kotlin.ByteArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Byte>): kotlin.ByteArray
public inline operator fun kotlin.CharArray.plus(/*0*/ element: kotlin.Char): kotlin.CharArray
public inline operator fun kotlin.CharArray.plus(/*0*/ elements: kotlin.CharArray): kotlin.CharArray
public operator fun kotlin.CharArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Char>): kotlin.CharArray
public inline operator fun kotlin.DoubleArray.plus(/*0*/ element: kotlin.Double): kotlin.DoubleArray
public inline operator fun kotlin.DoubleArray.plus(/*0*/ elements: kotlin.DoubleArray): kotlin.DoubleArray
public operator fun kotlin.DoubleArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Double>): kotlin.DoubleArray
public inline operator fun kotlin.FloatArray.plus(/*0*/ element: kotlin.Float): kotlin.FloatArray
public inline operator fun kotlin.FloatArray.plus(/*0*/ elements: kotlin.FloatArray): kotlin.FloatArray
public operator fun kotlin.FloatArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Float>): kotlin.FloatArray
public inline operator fun kotlin.IntArray.plus(/*0*/ element: kotlin.Int): kotlin.IntArray
public inline operator fun kotlin.IntArray.plus(/*0*/ elements: kotlin.IntArray): kotlin.IntArray
public operator fun kotlin.IntArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Int>): kotlin.IntArray
public inline operator fun kotlin.LongArray.plus(/*0*/ element: kotlin.Long): kotlin.LongArray
public inline operator fun kotlin.LongArray.plus(/*0*/ elements: kotlin.LongArray): kotlin.LongArray
public operator fun kotlin.LongArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Long>): kotlin.LongArray
public inline operator fun kotlin.ShortArray.plus(/*0*/ element: kotlin.Short): kotlin.ShortArray
public inline operator fun kotlin.ShortArray.plus(/*0*/ elements: kotlin.ShortArray): kotlin.ShortArray
public operator fun kotlin.ShortArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.Short>): kotlin.ShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.plus(/*0*/ element: kotlin.UByte): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UByteArray.plus(/*0*/ elements: kotlin.UByteArray): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.UByteArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.UByte>): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.plus(/*0*/ element: kotlin.UInt): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UIntArray.plus(/*0*/ elements: kotlin.UIntArray): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.UIntArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.UInt>): kotlin.UIntArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.plus(/*0*/ element: kotlin.ULong): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.ULongArray.plus(/*0*/ elements: kotlin.ULongArray): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.ULongArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.ULong>): kotlin.ULongArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.plus(/*0*/ element: kotlin.UShort): kotlin.UShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline operator fun kotlin.UShortArray.plus(/*0*/ elements: kotlin.UShortArray): kotlin.UShortArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public operator fun kotlin.UShortArray.plus(/*0*/ elements: kotlin.collections.Collection<kotlin.UShort>): kotlin.UShortArray
public operator fun </*0*/ T> kotlin.collections.Collection<T>.plus(/*0*/ element: T): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Collection<T>.plus(/*0*/ elements: kotlin.Array<out T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Collection<T>.plus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Collection<T>.plus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.plus(/*0*/ element: T): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.plus(/*0*/ elements: kotlin.Array<out T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.plus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.collections.List<T>
public operator fun </*0*/ T> kotlin.collections.Iterable<T>.plus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.collections.List<T>
public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.plus(/*0*/ pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>
public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.plus(/*0*/ pair: kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.plus(/*0*/ pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>
public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.plus(/*0*/ map: kotlin.collections.Map<out K, V>): kotlin.collections.Map<K, V>
public operator fun </*0*/ K, /*1*/ V> kotlin.collections.Map<out K, V>.plus(/*0*/ pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.collections.Map<K, V>
public operator fun </*0*/ T> kotlin.collections.Set<T>.plus(/*0*/ element: T): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.plus(/*0*/ elements: kotlin.Array<out T>): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.plus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.collections.Set<T>
public operator fun </*0*/ T> kotlin.collections.Set<T>.plus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.collections.Set<T>
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.plusAssign(/*0*/ element: T): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.plusAssign(/*0*/ elements: kotlin.Array<T>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.plusAssign(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ T> kotlin.collections.MutableCollection<in T>.plusAssign(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.plusAssign(/*0*/ pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.plusAssign(/*0*/ pair: kotlin.Pair<K, V>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.plusAssign(/*0*/ pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.plusAssign(/*0*/ map: kotlin.collections.Map<K, V>): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.plusAssign(/*0*/ pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.Unit
public inline fun </*0*/ T> kotlin.Array<out T>.plusElement(/*0*/ element: T): kotlin.Array<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>.plusElement(/*0*/ element: T): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Iterable<T>.plusElement(/*0*/ element: T): kotlin.collections.List<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Set<T>.plusElement(/*0*/ element: T): kotlin.collections.Set<T>
public fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.putAll(/*0*/ pairs: kotlin.Array<out kotlin.Pair<K, V>>): kotlin.Unit
public fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.putAll(/*0*/ pairs: kotlin.collections.Iterable<kotlin.Pair<K, V>>): kotlin.Unit
public fun </*0*/ K, /*1*/ V> kotlin.collections.MutableMap<in K, in V>.putAll(/*0*/ pairs: kotlin.sequences.Sequence<kotlin.Pair<K, V>>): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.random(): T
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> kotlin.Array<out T>.random(/*0*/ random: kotlin.random.Random): T
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.random(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.BooleanArray.random(/*0*/ random: kotlin.random.Random): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.random(): kotlin.Byte
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ByteArray.random(/*0*/ random: kotlin.random.Random): kotlin.Byte
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.random(): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.CharArray.random(/*0*/ random: kotlin.random.Random): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.random(): kotlin.Double
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.DoubleArray.random(/*0*/ random: kotlin.random.Random): kotlin.Double
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.random(): kotlin.Float
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.FloatArray.random(/*0*/ random: kotlin.random.Random): kotlin.Float
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.random(): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.IntArray.random(/*0*/ random: kotlin.random.Random): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.random(): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.LongArray.random(/*0*/ random: kotlin.random.Random): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.random(): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.ShortArray.random(/*0*/ random: kotlin.random.Random): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.random(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.random(/*0*/ random: kotlin.random.Random): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.random(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.random(/*0*/ random: kotlin.random.Random): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.random(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.random(/*0*/ random: kotlin.random.Random): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.random(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.random(/*0*/ random: kotlin.random.Random): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>.random(): T
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> kotlin.collections.Collection<T>.random(/*0*/ random: kotlin.random.Random): T
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.Array<out T>.randomOrNull(): T?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ T> kotlin.Array<out T>.randomOrNull(/*0*/ random: kotlin.random.Random): T?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.BooleanArray.randomOrNull(): kotlin.Boolean?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.BooleanArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Boolean?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.ByteArray.randomOrNull(): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ByteArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.CharArray.randomOrNull(): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.CharArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.DoubleArray.randomOrNull(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.DoubleArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Double?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.FloatArray.randomOrNull(): kotlin.Float?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.FloatArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Float?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.IntArray.randomOrNull(): kotlin.Int?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.IntArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Int?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.LongArray.randomOrNull(): kotlin.Long?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.LongArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Long?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.ShortArray.randomOrNull(): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ShortArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.randomOrNull(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByteArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.randomOrNull(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.UIntArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.randomOrNull(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULongArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.randomOrNull(): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShortArray.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.collections.Collection<T>.randomOrNull(): T?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ T> kotlin.collections.Collection<T>.randomOrNull(/*0*/ random: kotlin.random.Random): T?
public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduce(/*0*/ operation: (acc: S, T) -> S): S
public inline fun kotlin.BooleanArray.reduce(/*0*/ operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.ByteArray.reduce(/*0*/ operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte
public inline fun kotlin.CharArray.reduce(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char
public inline fun kotlin.DoubleArray.reduce(/*0*/ operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double
public inline fun kotlin.FloatArray.reduce(/*0*/ operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float
public inline fun kotlin.IntArray.reduce(/*0*/ operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int
public inline fun kotlin.LongArray.reduce(/*0*/ operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long
public inline fun kotlin.ShortArray.reduce(/*0*/ operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.reduce(/*0*/ operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.reduce(/*0*/ operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.reduce(/*0*/ operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.reduce(/*0*/ operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ S, /*1*/ T : S, /*2*/ K> kotlin.collections.Grouping<T, K>.reduce(/*0*/ operation: (key: K, accumulator: S, element: T) -> S): kotlin.collections.Map<K, S>
public inline fun </*0*/ S, /*1*/ T : S> kotlin.collections.Iterable<T>.reduce(/*0*/ operation: (acc: S, T) -> S): S
public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S
public inline fun kotlin.BooleanArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.ByteArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte
public inline fun kotlin.CharArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char
public inline fun kotlin.DoubleArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double
public inline fun kotlin.FloatArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float
public inline fun kotlin.IntArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int
public inline fun kotlin.LongArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long
public inline fun kotlin.ShortArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort
public inline fun </*0*/ S, /*1*/ T : S> kotlin.collections.Iterable<T>.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.BooleanArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.ByteArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.CharArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.DoubleArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.FloatArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.IntArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.LongArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long?
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.ShortArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short?
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.4") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ S, /*1*/ T : S> kotlin.collections.Iterable<T>.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduceOrNull(/*0*/ operation: (acc: S, T) -> S): S?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.BooleanArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Boolean, kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.ByteArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Byte, kotlin.Byte) -> kotlin.Byte): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.CharArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.DoubleArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Double, kotlin.Double) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.FloatArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Float, kotlin.Float) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.IntArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Int, kotlin.Int) -> kotlin.Int): kotlin.Int?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.LongArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Long, kotlin.Long) -> kotlin.Long): kotlin.Long?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.ShortArray.reduceOrNull(/*0*/ operation: (acc: kotlin.Short, kotlin.Short) -> kotlin.Short): kotlin.Short?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.reduceOrNull(/*0*/ operation: (acc: kotlin.UByte, kotlin.UByte) -> kotlin.UByte): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.reduceOrNull(/*0*/ operation: (acc: kotlin.UInt, kotlin.UInt) -> kotlin.UInt): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.reduceOrNull(/*0*/ operation: (acc: kotlin.ULong, kotlin.ULong) -> kotlin.ULong): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.reduceOrNull(/*0*/ operation: (acc: kotlin.UShort, kotlin.UShort) -> kotlin.UShort): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ S, /*1*/ T : S> kotlin.collections.Iterable<T>.reduceOrNull(/*0*/ operation: (acc: S, T) -> S): S?
public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduceRight(/*0*/ operation: (T, acc: S) -> S): S
public inline fun kotlin.BooleanArray.reduceRight(/*0*/ operation: (kotlin.Boolean, acc: kotlin.Boolean) -> kotlin.Boolean): kotlin.Boolean
public inline fun kotlin.ByteArray.reduceRight(/*0*/ operation: (kotlin.Byte, acc: kotlin.Byte) -> kotlin.Byte): kotlin.Byte
public inline fun kotlin.CharArray.reduceRight(/*0*/ operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char
public inline fun kotlin.DoubleArray.reduceRight(/*0*/ operation: (kotlin.Double, acc: kotlin.Double) -> kotlin.Double): kotlin.Double
public inline fun kotlin.FloatArray.reduceRight(/*0*/ operation: (kotlin.Float, acc: kotlin.Float) -> kotlin.Float): kotlin.Float
public inline fun kotlin.IntArray.reduceRight(/*0*/ operation: (kotlin.Int, acc: kotlin.Int) -> kotlin.Int): kotlin.Int
public inline fun kotlin.LongArray.reduceRight(/*0*/ operation: (kotlin.Long, acc: kotlin.Long) -> kotlin.Long): kotlin.Long
public inline fun kotlin.ShortArray.reduceRight(/*0*/ operation: (kotlin.Short, acc: kotlin.Short) -> kotlin.Short): kotlin.Short
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UByteArray.reduceRight(/*0*/ operation: (kotlin.UByte, acc: kotlin.UByte) -> kotlin.UByte): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UIntArray.reduceRight(/*0*/ operation: (kotlin.UInt, acc: kotlin.UInt) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.ULongArray.reduceRight(/*0*/ operation: (kotlin.ULong, acc: kotlin.ULong) -> kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.UShortArray.reduceRight(/*0*/ operation: (kotlin.UShort, acc: kotlin.UShort) -> kotlin.UShort): kotlin.UShort
public inline fun </*0*/ S, /*1*/ T : S> kotlin.collections.List<T>.reduceRight(/*0*/ operation: (T, acc: S) -> S): S
public inline fun </*0*/ S, /*1*/ T : S> kotlin.Array<out T>.reduceRightIndexed(/*0*/ operation: (index: kotlin.Int, T, acc: S) -> S): S
