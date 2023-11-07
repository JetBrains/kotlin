@kotlin.internal.InlineOnly
public inline fun <T> Sequence(crossinline iterator: () -> kotlin.collections.Iterator<T>): kotlin.sequences.Sequence<T>

public fun <T> emptySequence(): kotlin.sequences.Sequence<T>

public fun <T : kotlin.Any> generateSequence(nextFunction: () -> T?): kotlin.sequences.Sequence<T>

public fun <T : kotlin.Any> generateSequence(seedFunction: () -> T?, nextFunction: (T) -> T?): kotlin.sequences.Sequence<T>

@kotlin.internal.LowPriorityInOverloadResolution
public fun <T : kotlin.Any> generateSequence(seed: T?, nextFunction: (T) -> T?): kotlin.sequences.Sequence<T>

@kotlin.SinceKotlin(version = "1.3")
public fun <T> iterator(@kotlin.BuilderInference
block: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.collections.Iterator<T>

@kotlin.SinceKotlin(version = "1.3")
public fun <T> sequence(@kotlin.BuilderInference
block: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.sequences.Sequence<T>

public fun <T> sequenceOf(vararg elements: T): kotlin.sequences.Sequence<T>

public inline fun <T> kotlin.sequences.Sequence<T>.all(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.sequences.Sequence<T>.any(): kotlin.Boolean

public inline fun <T> kotlin.sequences.Sequence<T>.any(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

public fun <T> kotlin.sequences.Sequence<T>.asIterable(): kotlin.collections.Iterable<T>

public fun <T> kotlin.collections.Iterator<T>.asSequence(): kotlin.sequences.Sequence<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.asSequence(): kotlin.sequences.Sequence<T>

public inline fun <T, K, V> kotlin.sequences.Sequence<T>.associate(transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <T, K> kotlin.sequences.Sequence<T>.associateBy(keySelector: (T) -> K): kotlin.collections.Map<K, T>

public inline fun <T, K, V> kotlin.sequences.Sequence<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, V>

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, in T>> kotlin.sequences.Sequence<T>.associateByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<T>.associateTo(destination: M, transform: (T) -> kotlin.Pair<K, V>): M

@kotlin.SinceKotlin(version = "1.3")
public inline fun <K, V> kotlin.sequences.Sequence<K>.associateWith(valueSelector: (K) -> V): kotlin.collections.Map<K, V>

@kotlin.SinceKotlin(version = "1.3")
public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<K>.associateWithTo(destination: M, valueSelector: (K) -> V): M

@kotlin.jvm.JvmName(name = "averageOfByte")
public fun kotlin.sequences.Sequence<kotlin.Byte>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfDouble")
public fun kotlin.sequences.Sequence<kotlin.Double>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfFloat")
public fun kotlin.sequences.Sequence<kotlin.Float>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfInt")
public fun kotlin.sequences.Sequence<kotlin.Int>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfLong")
public fun kotlin.sequences.Sequence<kotlin.Long>.average(): kotlin.Double

@kotlin.jvm.JvmName(name = "averageOfShort")
public fun kotlin.sequences.Sequence<kotlin.Short>.average(): kotlin.Double

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.sequences.Sequence<T>.chunked(size: kotlin.Int): kotlin.sequences.Sequence<kotlin.collections.List<T>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T, R> kotlin.sequences.Sequence<T>.chunked(size: kotlin.Int, transform: (kotlin.collections.List<T>) -> R): kotlin.sequences.Sequence<R>

public fun <T> kotlin.sequences.Sequence<T>.constrainOnce(): kotlin.sequences.Sequence<T>

public operator fun <@kotlin.internal.OnlyInputTypes
T> kotlin.sequences.Sequence<T>.contains(element: T): kotlin.Boolean

public fun <T> kotlin.sequences.Sequence<T>.count(): kotlin.Int

public inline fun <T> kotlin.sequences.Sequence<T>.count(predicate: (T) -> kotlin.Boolean): kotlin.Int

public fun <T> kotlin.sequences.Sequence<T>.distinct(): kotlin.sequences.Sequence<T>

public fun <T, K> kotlin.sequences.Sequence<T>.distinctBy(selector: (T) -> K): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.drop(n: kotlin.Int): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.dropWhile(predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.elementAt(index: kotlin.Int): T

public fun <T> kotlin.sequences.Sequence<T>.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> T): T

public fun <T> kotlin.sequences.Sequence<T>.elementAtOrNull(index: kotlin.Int): T?

public fun <T> kotlin.sequences.Sequence<T>.filter(predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.filterIndexed(predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C

public inline fun <reified R> kotlin.sequences.Sequence<*>.filterIsInstance(): kotlin.sequences.Sequence<R>

public inline fun <reified R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<*>.filterIsInstanceTo(destination: C): C

public fun <T> kotlin.sequences.Sequence<T>.filterNot(predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>

public fun <T : kotlin.Any> kotlin.sequences.Sequence<T?>.filterNotNull(): kotlin.sequences.Sequence<T>

public fun <C : kotlin.collections.MutableCollection<in T>, T : kotlin.Any> kotlin.sequences.Sequence<T?>.filterNotNullTo(destination: C): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterNotTo(destination: C, predicate: (T) -> kotlin.Boolean): C

public inline fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterTo(destination: C, predicate: (T) -> kotlin.Boolean): C

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.find(predicate: (T) -> kotlin.Boolean): T?

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.findLast(predicate: (T) -> kotlin.Boolean): T?

public fun <T> kotlin.sequences.Sequence<T>.first(): T

public inline fun <T> kotlin.sequences.Sequence<T>.first(predicate: (T) -> kotlin.Boolean): T

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Any> kotlin.sequences.Sequence<T>.firstNotNullOf(transform: (T) -> R?): R

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Any> kotlin.sequences.Sequence<T>.firstNotNullOfOrNull(transform: (T) -> R?): R?

public fun <T> kotlin.sequences.Sequence<T>.firstOrNull(): T?

public inline fun <T> kotlin.sequences.Sequence<T>.firstOrNull(predicate: (T) -> kotlin.Boolean): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIterable")
public fun <T, R> kotlin.sequences.Sequence<T>.flatMap(transform: (T) -> kotlin.collections.Iterable<R>): kotlin.sequences.Sequence<R>

public fun <T, R> kotlin.sequences.Sequence<T>.flatMap(transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
public fun <T, R> kotlin.sequences.Sequence<T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequence")
public fun <T, R> kotlin.sequences.Sequence<T>.flatMapIndexed(transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.collections.Iterable<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedSequenceTo")
@kotlin.internal.InlineOnly
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> kotlin.sequences.Sequence<R>): C

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIterableTo")
public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapTo(destination: C, transform: (T) -> kotlin.collections.Iterable<R>): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapTo(destination: C, transform: (T) -> kotlin.sequences.Sequence<R>): C

@kotlin.jvm.JvmName(name = "flattenSequenceOfIterable")
public fun <T> kotlin.sequences.Sequence<kotlin.collections.Iterable<T>>.flatten(): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<kotlin.sequences.Sequence<T>>.flatten(): kotlin.sequences.Sequence<T>

public inline fun <T, R> kotlin.sequences.Sequence<T>.fold(initial: R, operation: (acc: R, T) -> R): R

public inline fun <T, R> kotlin.sequences.Sequence<T>.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): R

public inline fun <T> kotlin.sequences.Sequence<T>.forEach(action: (T) -> kotlin.Unit): kotlin.Unit

public inline fun <T> kotlin.sequences.Sequence<T>.forEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit

public inline fun <T, K> kotlin.sequences.Sequence<T>.groupBy(keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>

public inline fun <T, K, V> kotlin.sequences.Sequence<T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <T, K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.sequences.Sequence<T>.groupByTo(destination: M, keySelector: (T) -> K): M

public inline fun <T, K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.sequences.Sequence<T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M

@kotlin.SinceKotlin(version = "1.1")
public inline fun <T, K> kotlin.sequences.Sequence<T>.groupingBy(crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.sequences.Sequence<T>.ifEmpty(defaultValue: () -> kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.sequences.Sequence<T>.indexOf(element: T): kotlin.Int

public inline fun <T> kotlin.sequences.Sequence<T>.indexOfFirst(predicate: (T) -> kotlin.Boolean): kotlin.Int

public inline fun <T> kotlin.sequences.Sequence<T>.indexOfLast(predicate: (T) -> kotlin.Boolean): kotlin.Int

public fun <T, A : kotlin.text.Appendable> kotlin.sequences.Sequence<T>.joinTo(buffer: A, separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): A

public fun <T> kotlin.sequences.Sequence<T>.joinToString(separator: kotlin.CharSequence = ..., prefix: kotlin.CharSequence = ..., postfix: kotlin.CharSequence = ..., limit: kotlin.Int = ..., truncated: kotlin.CharSequence = ..., transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String

public fun <T> kotlin.sequences.Sequence<T>.last(): T

public inline fun <T> kotlin.sequences.Sequence<T>.last(predicate: (T) -> kotlin.Boolean): T

public fun <@kotlin.internal.OnlyInputTypes
T> kotlin.sequences.Sequence<T>.lastIndexOf(element: T): kotlin.Int

public fun <T> kotlin.sequences.Sequence<T>.lastOrNull(): T?

public inline fun <T> kotlin.sequences.Sequence<T>.lastOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun <T, R> kotlin.sequences.Sequence<T>.map(transform: (T) -> R): kotlin.sequences.Sequence<R>

public fun <T, R> kotlin.sequences.Sequence<T>.mapIndexed(transform: (index: kotlin.Int, T) -> R): kotlin.sequences.Sequence<R>

public fun <T, R : kotlin.Any> kotlin.sequences.Sequence<T>.mapIndexedNotNull(transform: (index: kotlin.Int, T) -> R?): kotlin.sequences.Sequence<R>

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapIndexedNotNullTo(destination: C, transform: (index: kotlin.Int, T) -> R?): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapIndexedTo(destination: C, transform: (index: kotlin.Int, T) -> R): C

public fun <T, R : kotlin.Any> kotlin.sequences.Sequence<T>.mapNotNull(transform: (T) -> R?): kotlin.sequences.Sequence<R>

public inline fun <T, R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapNotNullTo(destination: C, transform: (T) -> R?): C

public inline fun <T, R, C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapTo(destination: C, transform: (T) -> R): C

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxOrThrow")
public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.max(): T

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxOrThrow")
public fun kotlin.sequences.Sequence<kotlin.Double>.max(): kotlin.Double

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxOrThrow")
public fun kotlin.sequences.Sequence<kotlin.Float>.max(): kotlin.Float

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxByOrThrow")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxBy(selector: (T) -> R): T

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.maxOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.maxOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.maxOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.maxOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.sequences.Sequence<T>.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.sequences.Sequence<T>.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.maxOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.sequences.Sequence<kotlin.Double>.maxOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.sequences.Sequence<kotlin.Float>.maxOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxWithOrThrow")
public fun <T> kotlin.sequences.Sequence<T>.maxWith(comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.sequences.Sequence<T>.maxWithOrNull(comparator: kotlin.Comparator<in T>): T?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minOrThrow")
public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.min(): T

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minOrThrow")
public fun kotlin.sequences.Sequence<kotlin.Double>.min(): kotlin.Double

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minOrThrow")
public fun kotlin.sequences.Sequence<kotlin.Float>.min(): kotlin.Float

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minByOrThrow")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minBy(selector: (T) -> R): T

@kotlin.SinceKotlin(version = "1.4")
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minByOrNull(selector: (T) -> R): T?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minOf(selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.minOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.minOf(selector: (T) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minOfOrNull(selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.minOfOrNull(selector: (T) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.minOfOrNull(selector: (T) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.sequences.Sequence<T>.minOfWith(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <T, R> kotlin.sequences.Sequence<T>.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (T) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.minOrNull(): T?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.sequences.Sequence<kotlin.Double>.minOrNull(): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.sequences.Sequence<kotlin.Float>.minOrNull(): kotlin.Float?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minWithOrThrow")
public fun <T> kotlin.sequences.Sequence<T>.minWith(comparator: kotlin.Comparator<in T>): T

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.sequences.Sequence<T>.minWithOrNull(comparator: kotlin.Comparator<in T>): T?

public operator fun <T> kotlin.sequences.Sequence<T>.minus(element: T): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.minus(elements: kotlin.Array<out T>): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.minus(elements: kotlin.collections.Iterable<T>): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.minus(elements: kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.minusElement(element: T): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.none(): kotlin.Boolean

public inline fun <T> kotlin.sequences.Sequence<T>.none(predicate: (T) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
public fun <T> kotlin.sequences.Sequence<T>.onEach(action: (T) -> kotlin.Unit): kotlin.sequences.Sequence<T>

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.sequences.Sequence<T>.onEachIndexed(action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.sequences.Sequence<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>?.orEmpty(): kotlin.sequences.Sequence<T>

public inline fun <T> kotlin.sequences.Sequence<T>.partition(predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>

public operator fun <T> kotlin.sequences.Sequence<T>.plus(element: T): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.plus(elements: kotlin.Array<out T>): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.plus(elements: kotlin.collections.Iterable<T>): kotlin.sequences.Sequence<T>

public operator fun <T> kotlin.sequences.Sequence<T>.plus(elements: kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>

@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.plusElement(element: T): kotlin.sequences.Sequence<T>

public inline fun <S, T : S> kotlin.sequences.Sequence<T>.reduce(operation: (acc: S, T) -> S): S

public inline fun <S, T : S> kotlin.sequences.Sequence<T>.reduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): S

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.sequences.Sequence<T>.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: S, T) -> S): S?

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S, T : S> kotlin.sequences.Sequence<T>.reduceOrNull(operation: (acc: S, T) -> S): S?

public fun <T : kotlin.Any> kotlin.sequences.Sequence<T?>.requireNoNulls(): kotlin.sequences.Sequence<T>

@kotlin.SinceKotlin(version = "1.4")
public fun <T, R> kotlin.sequences.Sequence<T>.runningFold(initial: R, operation: (acc: R, T) -> R): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
public fun <T, R> kotlin.sequences.Sequence<T>.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
public fun <S, T : S> kotlin.sequences.Sequence<T>.runningReduce(operation: (acc: S, T) -> S): kotlin.sequences.Sequence<S>

@kotlin.SinceKotlin(version = "1.4")
public fun <S, T : S> kotlin.sequences.Sequence<T>.runningReduceIndexed(operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.sequences.Sequence<S>

@kotlin.SinceKotlin(version = "1.4")
public fun <T, R> kotlin.sequences.Sequence<T>.scan(initial: R, operation: (acc: R, T) -> R): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
public fun <T, R> kotlin.sequences.Sequence<T>.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.sequences.Sequence<T>.shuffled(): kotlin.sequences.Sequence<T>

@kotlin.SinceKotlin(version = "1.4")
public fun <T> kotlin.sequences.Sequence<T>.shuffled(random: kotlin.random.Random): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.single(): T

public inline fun <T> kotlin.sequences.Sequence<T>.single(predicate: (T) -> kotlin.Boolean): T

public fun <T> kotlin.sequences.Sequence<T>.singleOrNull(): T?

public inline fun <T> kotlin.sequences.Sequence<T>.singleOrNull(predicate: (T) -> kotlin.Boolean): T?

public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.sorted(): kotlin.sequences.Sequence<T>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.sortedBy(crossinline selector: (T) -> R?): kotlin.sequences.Sequence<T>

public inline fun <T, R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.sortedByDescending(crossinline selector: (T) -> R?): kotlin.sequences.Sequence<T>

public fun <T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.sortedDescending(): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.sortedWith(comparator: kotlin.Comparator<in T>): kotlin.sequences.Sequence<T>

@kotlin.jvm.JvmName(name = "sumOfByte")
public fun kotlin.sequences.Sequence<kotlin.Byte>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfDouble")
public fun kotlin.sequences.Sequence<kotlin.Double>.sum(): kotlin.Double

@kotlin.jvm.JvmName(name = "sumOfFloat")
public fun kotlin.sequences.Sequence<kotlin.Float>.sum(): kotlin.Float

@kotlin.jvm.JvmName(name = "sumOfInt")
public fun kotlin.sequences.Sequence<kotlin.Int>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfLong")
public fun kotlin.sequences.Sequence<kotlin.Long>.sum(): kotlin.Long

@kotlin.jvm.JvmName(name = "sumOfShort")
public fun kotlin.sequences.Sequence<kotlin.Short>.sum(): kotlin.Int

@kotlin.jvm.JvmName(name = "sumOfUByte")
@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.sequences.Sequence<kotlin.UByte>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.sequences.Sequence<kotlin.UInt>.sum(): kotlin.UInt

@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.sequences.Sequence<kotlin.ULong>.sum(): kotlin.ULong

@kotlin.jvm.JvmName(name = "sumOfUShort")
@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.sequences.Sequence<kotlin.UShort>.sum(): kotlin.UInt

@kotlin.Deprecated(message = "Use sumOf instead.", replaceWith = kotlin.ReplaceWith(expression = "this.sumOf(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public inline fun <T> kotlin.sequences.Sequence<T>.sumBy(selector: (T) -> kotlin.Int): kotlin.Int

@kotlin.Deprecated(message = "Use sumOf instead.", replaceWith = kotlin.ReplaceWith(expression = "this.sumOf(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public inline fun <T> kotlin.sequences.Sequence<T>.sumByDouble(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.sumOf(selector: (T) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.sumOf(selector: (T) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.sumOf(selector: (T) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.sumOf(selector: (T) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.sequences.Sequence<T>.sumOf(selector: (T) -> kotlin.ULong): kotlin.ULong

public fun <T> kotlin.sequences.Sequence<T>.take(n: kotlin.Int): kotlin.sequences.Sequence<T>

public fun <T> kotlin.sequences.Sequence<T>.takeWhile(predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>

public fun <T, C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.toCollection(destination: C): C

public fun <T> kotlin.sequences.Sequence<T>.toHashSet(): kotlin.collections.HashSet<T>

public fun <T> kotlin.sequences.Sequence<T>.toList(): kotlin.collections.List<T>

public fun <T> kotlin.sequences.Sequence<T>.toMutableList(): kotlin.collections.MutableList<T>

public fun <T> kotlin.sequences.Sequence<T>.toMutableSet(): kotlin.collections.MutableSet<T>

public fun <T> kotlin.sequences.Sequence<T>.toSet(): kotlin.collections.Set<T>

public fun <T, R> kotlin.sequences.Sequence<kotlin.Pair<T, R>>.unzip(): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<R>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.sequences.Sequence<T>.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ...): kotlin.sequences.Sequence<kotlin.collections.List<T>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T, R> kotlin.sequences.Sequence<T>.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ..., transform: (kotlin.collections.List<T>) -> R): kotlin.sequences.Sequence<R>

public fun <T> kotlin.sequences.Sequence<T>.withIndex(): kotlin.sequences.Sequence<kotlin.collections.IndexedValue<T>>

public infix fun <T, R> kotlin.sequences.Sequence<T>.zip(other: kotlin.sequences.Sequence<R>): kotlin.sequences.Sequence<kotlin.Pair<T, R>>

public fun <T, R, V> kotlin.sequences.Sequence<T>.zip(other: kotlin.sequences.Sequence<R>, transform: (a: T, b: R) -> V): kotlin.sequences.Sequence<V>

@kotlin.SinceKotlin(version = "1.2")
public fun <T> kotlin.sequences.Sequence<T>.zipWithNext(): kotlin.sequences.Sequence<kotlin.Pair<T, T>>

@kotlin.SinceKotlin(version = "1.2")
public fun <T, R> kotlin.sequences.Sequence<T>.zipWithNext(transform: (a: T, b: T) -> R): kotlin.sequences.Sequence<R>

public interface Sequence<out T> {
    public abstract operator fun iterator(): kotlin.collections.Iterator<T>
}

@kotlin.coroutines.RestrictsSuspension
@kotlin.SinceKotlin(version = "1.3")
public abstract class SequenceScope<in T> {
    public abstract suspend fun yield(value: T): kotlin.Unit

    public final suspend fun yieldAll(elements: kotlin.collections.Iterable<T>): kotlin.Unit

    public abstract suspend fun yieldAll(iterator: kotlin.collections.Iterator<T>): kotlin.Unit

    public final suspend fun yieldAll(sequence: kotlin.sequences.Sequence<T>): kotlin.Unit
}