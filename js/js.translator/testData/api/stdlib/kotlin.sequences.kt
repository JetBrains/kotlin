package kotlin.sequences

@kotlin.internal.InlineOnly public inline fun </*0*/ T> Sequence(/*0*/ crossinline iterator: () -> kotlin.collections.Iterator<T>): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use 'iterator { }' function instead.", replaceWith = kotlin.ReplaceWith(expression = "iterator(builderAction)", imports = {})) @kotlin.internal.InlineOnly public inline fun </*0*/ T> buildIterator(/*0*/ @kotlin.BuilderInference noinline builderAction: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.collections.Iterator<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use 'sequence { }' function instead.", replaceWith = kotlin.ReplaceWith(expression = "sequence(builderAction)", imports = {})) @kotlin.internal.InlineOnly public inline fun </*0*/ T> buildSequence(/*0*/ @kotlin.BuilderInference noinline builderAction: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.sequences.Sequence<T>
public fun </*0*/ T> emptySequence(): kotlin.sequences.Sequence<T>
public fun </*0*/ T : kotlin.Any> generateSequence(/*0*/ nextFunction: () -> T?): kotlin.sequences.Sequence<T>
public fun </*0*/ T : kotlin.Any> generateSequence(/*0*/ seedFunction: () -> T?, /*1*/ nextFunction: (T) -> T?): kotlin.sequences.Sequence<T>
@kotlin.internal.LowPriorityInOverloadResolution public fun </*0*/ T : kotlin.Any> generateSequence(/*0*/ seed: T?, /*1*/ nextFunction: (T) -> T?): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> iterator(/*0*/ @kotlin.BuilderInference block: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.collections.Iterator<T>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> sequence(/*0*/ @kotlin.BuilderInference block: suspend kotlin.sequences.SequenceScope<T>.() -> kotlin.Unit): kotlin.sequences.Sequence<T>
public fun </*0*/ T> sequenceOf(/*0*/ vararg elements: T /*kotlin.Array<out T>*/): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.all(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T> kotlin.sequences.Sequence<T>.any(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.any(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T> kotlin.sequences.Sequence<T>.asIterable(): kotlin.collections.Iterable<T>
public fun </*0*/ T> kotlin.collections.Iterator<T>.asSequence(): kotlin.sequences.Sequence<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.asSequence(): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.sequences.Sequence<T>.associate(/*0*/ transform: (T) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K> kotlin.sequences.Sequence<T>.associateBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, T>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.sequences.Sequence<T>.associateBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, in T>> kotlin.sequences.Sequence<T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<T>.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<T>.associateTo(/*0*/ destination: M, /*1*/ transform: (T) -> kotlin.Pair<K, V>): M
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ K, /*1*/ V> kotlin.sequences.Sequence<K>.associateWith(/*0*/ valueSelector: (K) -> V): kotlin.collections.Map<K, V>
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.sequences.Sequence<K>.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (K) -> V): M
@kotlin.jvm.JvmName(name = "averageOfByte") public fun kotlin.sequences.Sequence<kotlin.Byte>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfDouble") public fun kotlin.sequences.Sequence<kotlin.Double>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfFloat") public fun kotlin.sequences.Sequence<kotlin.Float>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfInt") public fun kotlin.sequences.Sequence<kotlin.Int>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfLong") public fun kotlin.sequences.Sequence<kotlin.Long>.average(): kotlin.Double
@kotlin.jvm.JvmName(name = "averageOfShort") public fun kotlin.sequences.Sequence<kotlin.Short>.average(): kotlin.Double
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T> kotlin.sequences.Sequence<T>.chunked(/*0*/ size: kotlin.Int): kotlin.sequences.Sequence<kotlin.collections.List<T>>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.chunked(/*0*/ size: kotlin.Int, /*1*/ transform: (kotlin.collections.List<T>) -> R): kotlin.sequences.Sequence<R>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.constrainOnce(): kotlin.sequences.Sequence<T>
public operator fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.sequences.Sequence<T>.contains(/*0*/ element: T): kotlin.Boolean
public fun </*0*/ T> kotlin.sequences.Sequence<T>.count(): kotlin.Int
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.count(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public fun </*0*/ T> kotlin.sequences.Sequence<T>.distinct(): kotlin.sequences.Sequence<T>
public fun </*0*/ T, /*1*/ K> kotlin.sequences.Sequence<T>.distinctBy(/*0*/ selector: (T) -> K): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.drop(/*0*/ n: kotlin.Int): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.dropWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.elementAt(/*0*/ index: kotlin.Int): T
public fun </*0*/ T> kotlin.sequences.Sequence<T>.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> T): T
public fun </*0*/ T> kotlin.sequences.Sequence<T>.elementAtOrNull(/*0*/ index: kotlin.Int): T?
public fun </*0*/ T> kotlin.sequences.Sequence<T>.filter(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.filterIndexed(/*0*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, T) -> kotlin.Boolean): C
public inline fun </*0*/ reified R> kotlin.sequences.Sequence<*>.filterIsInstance(): kotlin.sequences.Sequence<R>
public inline fun </*0*/ reified R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<*>.filterIsInstanceTo(/*0*/ destination: C): C
public fun </*0*/ T> kotlin.sequences.Sequence<T>.filterNot(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>
public fun </*0*/ T : kotlin.Any> kotlin.sequences.Sequence<T?>.filterNotNull(): kotlin.sequences.Sequence<T>
public fun </*0*/ C : kotlin.collections.MutableCollection<in T>, /*1*/ T : kotlin.Any> kotlin.sequences.Sequence<T?>.filterNotNullTo(/*0*/ destination: C): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
public inline fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.filterTo(/*0*/ destination: C, /*1*/ predicate: (T) -> kotlin.Boolean): C
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.find(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.findLast(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T> kotlin.sequences.Sequence<T>.first(): T
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.first(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ T> kotlin.sequences.Sequence<T>.firstOrNull(): T?
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.firstOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapIterable") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.flatMap(/*0*/ transform: (T) -> kotlin.collections.Iterable<R>): kotlin.sequences.Sequence<R>
public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.flatMap(/*0*/ transform: (T) -> kotlin.sequences.Sequence<R>): kotlin.sequences.Sequence<R>
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "flatMapIterableTo") public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.flatMapTo(/*0*/ destination: C, /*1*/ transform: (T) -> kotlin.sequences.Sequence<R>): C
@kotlin.jvm.JvmName(name = "flattenSequenceOfIterable") public fun </*0*/ T> kotlin.sequences.Sequence<kotlin.collections.Iterable<T>>.flatten(): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<kotlin.sequences.Sequence<T>>.flatten(): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, T) -> R): R
public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, T) -> R): R
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.forEach(/*0*/ action: (T) -> kotlin.Unit): kotlin.Unit
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.forEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.Unit
public inline fun </*0*/ T, /*1*/ K> kotlin.sequences.Sequence<T>.groupBy(/*0*/ keySelector: (T) -> K): kotlin.collections.Map<K, kotlin.collections.List<T>>
public inline fun </*0*/ T, /*1*/ K, /*2*/ V> kotlin.sequences.Sequence<T>.groupBy(/*0*/ keySelector: (T) -> K, /*1*/ valueTransform: (T) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ T, /*1*/ K, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<T>>> kotlin.sequences.Sequence<T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K): M
public inline fun </*0*/ T, /*1*/ K, /*2*/ V, /*3*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.sequences.Sequence<T>.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (T) -> K, /*2*/ valueTransform: (T) -> V): M
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ T, /*1*/ K> kotlin.sequences.Sequence<T>.groupingBy(/*0*/ crossinline keySelector: (T) -> K): kotlin.collections.Grouping<T, K>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> kotlin.sequences.Sequence<T>.ifEmpty(/*0*/ defaultValue: () -> kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.sequences.Sequence<T>.indexOf(/*0*/ element: T): kotlin.Int
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.indexOfFirst(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.indexOfLast(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Int
public fun </*0*/ T, /*1*/ A : kotlin.text.Appendable> kotlin.sequences.Sequence<T>.joinTo(/*0*/ buffer: A, /*1*/ separator: kotlin.CharSequence = ..., /*2*/ prefix: kotlin.CharSequence = ..., /*3*/ postfix: kotlin.CharSequence = ..., /*4*/ limit: kotlin.Int = ..., /*5*/ truncated: kotlin.CharSequence = ..., /*6*/ transform: ((T) -> kotlin.CharSequence)? = ...): A
public fun </*0*/ T> kotlin.sequences.Sequence<T>.joinToString(/*0*/ separator: kotlin.CharSequence = ..., /*1*/ prefix: kotlin.CharSequence = ..., /*2*/ postfix: kotlin.CharSequence = ..., /*3*/ limit: kotlin.Int = ..., /*4*/ truncated: kotlin.CharSequence = ..., /*5*/ transform: ((T) -> kotlin.CharSequence)? = ...): kotlin.String
public fun </*0*/ T> kotlin.sequences.Sequence<T>.last(): T
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.last(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ @kotlin.internal.OnlyInputTypes T> kotlin.sequences.Sequence<T>.lastIndexOf(/*0*/ element: T): kotlin.Int
public fun </*0*/ T> kotlin.sequences.Sequence<T>.lastOrNull(): T?
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.lastOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.map(/*0*/ transform: (T) -> R): kotlin.sequences.Sequence<R>
public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.mapIndexed(/*0*/ transform: (index: kotlin.Int, T) -> R): kotlin.sequences.Sequence<R>
public fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.sequences.Sequence<T>.mapIndexedNotNull(/*0*/ transform: (index: kotlin.Int, T) -> R?): kotlin.sequences.Sequence<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapIndexedNotNullTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R?): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, T) -> R): C
public fun </*0*/ T, /*1*/ R : kotlin.Any> kotlin.sequences.Sequence<T>.mapNotNull(/*0*/ transform: (T) -> R?): kotlin.sequences.Sequence<R>
public inline fun </*0*/ T, /*1*/ R : kotlin.Any, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapNotNullTo(/*0*/ destination: C, /*1*/ transform: (T) -> R?): C
public inline fun </*0*/ T, /*1*/ R, /*2*/ C : kotlin.collections.MutableCollection<in R>> kotlin.sequences.Sequence<T>.mapTo(/*0*/ destination: C, /*1*/ transform: (T) -> R): C
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.max(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.sequences.Sequence<kotlin.Double>.max(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.sequences.Sequence<kotlin.Float>.max(): kotlin.Float?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxBy(/*0*/ selector: (T) -> R): T?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.maxOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.maxOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.maxOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.maxOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
public fun </*0*/ T> kotlin.sequences.Sequence<T>.maxWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.min(): T?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.sequences.Sequence<kotlin.Double>.min(): kotlin.Double?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.sequences.Sequence<kotlin.Float>.min(): kotlin.Float?
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minBy(/*0*/ selector: (T) -> R): T?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minOf(/*0*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.minOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.minOf(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.minOfOrNull(/*0*/ selector: (T) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.minOfOrNull(/*0*/ selector: (T) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (T) -> R): R?
public fun </*0*/ T> kotlin.sequences.Sequence<T>.minWith(/*0*/ comparator: kotlin.Comparator<in T>): T?
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.minus(/*0*/ element: T): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.minus(/*0*/ elements: kotlin.Array<out T>): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.minus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.minus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.minusElement(/*0*/ element: T): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.none(): kotlin.Boolean
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.none(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") public fun </*0*/ T> kotlin.sequences.Sequence<T>.onEach(/*0*/ action: (T) -> kotlin.Unit): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T> kotlin.sequences.Sequence<T>.onEachIndexed(/*0*/ action: (index: kotlin.Int, T) -> kotlin.Unit): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>?.orEmpty(): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.partition(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<T>>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.plus(/*0*/ element: T): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.plus(/*0*/ elements: kotlin.Array<out T>): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.plus(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.sequences.Sequence<T>
public operator fun </*0*/ T> kotlin.sequences.Sequence<T>.plus(/*0*/ elements: kotlin.sequences.Sequence<T>): kotlin.sequences.Sequence<T>
@kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.plusElement(/*0*/ element: T): kotlin.sequences.Sequence<T>
public inline fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.reduce(/*0*/ operation: (acc: S, T) -> S): S
public inline fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): S?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.reduceOrNull(/*0*/ operation: (acc: S, T) -> S): S?
public fun </*0*/ T : kotlin.Any> kotlin.sequences.Sequence<T?>.requireNoNulls(): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.runningFold(/*0*/ initial: R, /*1*/ operation: (acc: R, T) -> R): kotlin.sequences.Sequence<R>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.runningFoldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.sequences.Sequence<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.runningReduce(/*0*/ operation: (acc: S, T) -> S): kotlin.sequences.Sequence<S>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.runningReduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.sequences.Sequence<S>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.scan(/*0*/ initial: R, /*1*/ operation: (acc: R, T) -> R): kotlin.sequences.Sequence<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.scanIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, T) -> R): kotlin.sequences.Sequence<R>
@kotlin.Deprecated(message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {})) @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.scanReduce(/*0*/ operation: (acc: S, T) -> S): kotlin.sequences.Sequence<S>
@kotlin.Deprecated(message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {})) @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ S, /*1*/ T : S> kotlin.sequences.Sequence<T>.scanReduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: S, T) -> S): kotlin.sequences.Sequence<S>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T> kotlin.sequences.Sequence<T>.shuffled(): kotlin.sequences.Sequence<T>
@kotlin.SinceKotlin(version = "1.4") public fun </*0*/ T> kotlin.sequences.Sequence<T>.shuffled(/*0*/ random: kotlin.random.Random): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.single(): T
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.single(/*0*/ predicate: (T) -> kotlin.Boolean): T
public fun </*0*/ T> kotlin.sequences.Sequence<T>.singleOrNull(): T?
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.singleOrNull(/*0*/ predicate: (T) -> kotlin.Boolean): T?
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.sorted(): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.sortedBy(/*0*/ crossinline selector: (T) -> R?): kotlin.sequences.Sequence<T>
public inline fun </*0*/ T, /*1*/ R : kotlin.Comparable<R>> kotlin.sequences.Sequence<T>.sortedByDescending(/*0*/ crossinline selector: (T) -> R?): kotlin.sequences.Sequence<T>
public fun </*0*/ T : kotlin.Comparable<T>> kotlin.sequences.Sequence<T>.sortedDescending(): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.sortedWith(/*0*/ comparator: kotlin.Comparator<in T>): kotlin.sequences.Sequence<T>
@kotlin.jvm.JvmName(name = "sumOfByte") public fun kotlin.sequences.Sequence<kotlin.Byte>.sum(): kotlin.Int
@kotlin.jvm.JvmName(name = "sumOfDouble") public fun kotlin.sequences.Sequence<kotlin.Double>.sum(): kotlin.Double
@kotlin.jvm.JvmName(name = "sumOfFloat") public fun kotlin.sequences.Sequence<kotlin.Float>.sum(): kotlin.Float
@kotlin.jvm.JvmName(name = "sumOfInt") public fun kotlin.sequences.Sequence<kotlin.Int>.sum(): kotlin.Int
@kotlin.jvm.JvmName(name = "sumOfLong") public fun kotlin.sequences.Sequence<kotlin.Long>.sum(): kotlin.Long
@kotlin.jvm.JvmName(name = "sumOfShort") public fun kotlin.sequences.Sequence<kotlin.Short>.sum(): kotlin.Int
@kotlin.jvm.JvmName(name = "sumOfUByte") @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.sequences.Sequence<kotlin.UByte>.sum(): kotlin.UInt
@kotlin.jvm.JvmName(name = "sumOfUInt") @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.sequences.Sequence<kotlin.UInt>.sum(): kotlin.UInt
@kotlin.jvm.JvmName(name = "sumOfULong") @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.sequences.Sequence<kotlin.ULong>.sum(): kotlin.ULong
@kotlin.jvm.JvmName(name = "sumOfUShort") @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.sequences.Sequence<kotlin.UShort>.sum(): kotlin.UInt
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumBy(/*0*/ selector: (T) -> kotlin.Int): kotlin.Int
public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumByDouble(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfDouble") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumOf(/*0*/ selector: (T) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfInt") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumOf(/*0*/ selector: (T) -> kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfLong") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumOf(/*0*/ selector: (T) -> kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfUInt") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumOf(/*0*/ selector: (T) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfULong") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.sequences.Sequence<T>.sumOf(/*0*/ selector: (T) -> kotlin.ULong): kotlin.ULong
public fun </*0*/ T> kotlin.sequences.Sequence<T>.take(/*0*/ n: kotlin.Int): kotlin.sequences.Sequence<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.takeWhile(/*0*/ predicate: (T) -> kotlin.Boolean): kotlin.sequences.Sequence<T>
public fun </*0*/ T, /*1*/ C : kotlin.collections.MutableCollection<in T>> kotlin.sequences.Sequence<T>.toCollection(/*0*/ destination: C): C
public fun </*0*/ T> kotlin.sequences.Sequence<T>.toHashSet(): kotlin.collections.HashSet<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.toList(): kotlin.collections.List<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.toMutableList(): kotlin.collections.MutableList<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.toMutableSet(): kotlin.collections.MutableSet<T>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.toSet(): kotlin.collections.Set<T>
public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<kotlin.Pair<T, R>>.unzip(): kotlin.Pair<kotlin.collections.List<T>, kotlin.collections.List<R>>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T> kotlin.sequences.Sequence<T>.windowed(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ...): kotlin.sequences.Sequence<kotlin.collections.List<T>>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.windowed(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ..., /*3*/ transform: (kotlin.collections.List<T>) -> R): kotlin.sequences.Sequence<R>
public fun </*0*/ T> kotlin.sequences.Sequence<T>.withIndex(): kotlin.sequences.Sequence<kotlin.collections.IndexedValue<T>>
public infix fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.zip(/*0*/ other: kotlin.sequences.Sequence<R>): kotlin.sequences.Sequence<kotlin.Pair<T, R>>
public fun </*0*/ T, /*1*/ R, /*2*/ V> kotlin.sequences.Sequence<T>.zip(/*0*/ other: kotlin.sequences.Sequence<R>, /*1*/ transform: (a: T, b: R) -> V): kotlin.sequences.Sequence<V>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T> kotlin.sequences.Sequence<T>.zipWithNext(): kotlin.sequences.Sequence<kotlin.Pair<T, T>>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ T, /*1*/ R> kotlin.sequences.Sequence<T>.zipWithNext(/*0*/ transform: (a: T, b: T) -> R): kotlin.sequences.Sequence<R>

public interface Sequence</*0*/ out T> {
    public abstract operator fun iterator(): kotlin.collections.Iterator<T>
}

@kotlin.coroutines.RestrictsSuspension @kotlin.SinceKotlin(version = "1.3") public abstract class SequenceScope</*0*/ in T> {
    public abstract suspend fun yield(/*0*/ value: T): kotlin.Unit
    public final suspend fun yieldAll(/*0*/ elements: kotlin.collections.Iterable<T>): kotlin.Unit
    public abstract suspend fun yieldAll(/*0*/ iterator: kotlin.collections.Iterator<T>): kotlin.Unit
    public final suspend fun yieldAll(/*0*/ sequence: kotlin.sequences.Sequence<T>): kotlin.Unit
}
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use SequenceScope class instead.", replaceWith = kotlin.ReplaceWith(expression = "SequenceScope<T>", imports = {})) public typealias SequenceBuilder</*0*/ T> = kotlin.sequences.SequenceScope<T>