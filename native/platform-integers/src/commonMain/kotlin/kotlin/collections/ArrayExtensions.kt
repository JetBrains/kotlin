@file:Suppress("NO_ACTUAL_FOR_EXPECT", "unused", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS")

package kotlin.collections

import kotlin.random.Random

expect inline operator fun PlatformIntArray.component1(): PlatformInt
expect inline operator fun PlatformIntArray.component2(): PlatformInt
expect inline operator fun PlatformIntArray.component3(): PlatformInt
expect inline operator fun PlatformIntArray.component4(): PlatformInt
expect inline operator fun PlatformIntArray.component5(): PlatformInt
expect operator fun PlatformIntArray.contains(element: PlatformInt): Boolean
expect fun PlatformIntArray.elementAt(index: Int): PlatformInt
expect inline fun PlatformIntArray.elementAtOrElse(index: Int, defaultValue: (Int) -> PlatformInt): PlatformInt
expect inline fun PlatformIntArray.elementAtOrNull(index: Int): PlatformInt?
expect inline fun PlatformIntArray.find(predicate: (PlatformInt) -> Boolean): PlatformInt?
expect inline fun PlatformIntArray.findLast(predicate: (PlatformInt) -> Boolean): PlatformInt?
expect fun PlatformIntArray.first(): PlatformInt
expect inline fun PlatformIntArray.first(predicate: (PlatformInt) -> Boolean): PlatformInt
expect fun PlatformIntArray.firstOrNull(): PlatformInt?
expect inline fun PlatformIntArray.firstOrNull(predicate: (PlatformInt) -> Boolean): PlatformInt?
expect inline fun PlatformIntArray.getOrElse(index: Int, defaultValue: (Int) -> PlatformInt): PlatformInt
expect fun PlatformIntArray.getOrNull(index: Int): PlatformInt?
expect fun PlatformIntArray.indexOf(element: PlatformInt): Int
expect inline fun PlatformIntArray.indexOfFirst(predicate: (PlatformInt) -> Boolean): Int
expect inline fun PlatformIntArray.indexOfLast(predicate: (PlatformInt) -> Boolean): Int
expect fun PlatformIntArray.last(): PlatformInt
expect inline fun PlatformIntArray.last(predicate: (PlatformInt) -> Boolean): PlatformInt
expect fun PlatformIntArray.lastIndexOf(element: PlatformInt): Int
expect fun PlatformIntArray.lastOrNull(): PlatformInt?
expect inline fun PlatformIntArray.lastOrNull(predicate: (PlatformInt) -> Boolean): PlatformInt?
expect inline fun PlatformIntArray.random(): PlatformInt
expect fun PlatformIntArray.random(random: Random): PlatformInt
expect inline fun PlatformIntArray.randomOrNull(): PlatformInt?
expect fun PlatformIntArray.randomOrNull(random: Random): PlatformInt?
expect fun PlatformIntArray.single(): PlatformInt
expect inline fun PlatformIntArray.single(predicate: (PlatformInt) -> Boolean): PlatformInt
expect fun PlatformIntArray.singleOrNull(): PlatformInt?
expect inline fun PlatformIntArray.singleOrNull(predicate: (PlatformInt) -> Boolean): PlatformInt?
expect fun PlatformIntArray.drop(n: Int): List<PlatformInt>
expect fun PlatformIntArray.dropLast(n: Int): List<PlatformInt>
expect inline fun PlatformIntArray.dropLastWhile(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun PlatformIntArray.dropWhile(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun PlatformIntArray.filter(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun PlatformIntArray.filterIndexed(predicate: (index: Int, PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun <C : MutableCollection<in PlatformInt>> PlatformIntArray.filterIndexedTo(
    destination: C,
    predicate: (index: Int, PlatformInt) -> Boolean
): C
expect inline fun PlatformIntArray.filterNot(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun <C : MutableCollection<in PlatformInt>> PlatformIntArray.filterNotTo(
    destination: C,
    predicate: (PlatformInt) -> Boolean
): C
expect inline fun <C : MutableCollection<in PlatformInt>> PlatformIntArray.filterTo(
    destination: C,
    predicate: (PlatformInt) -> Boolean
): C
expect fun PlatformIntArray.slice(indices: IntRange): List<PlatformInt>
expect fun PlatformIntArray.slice(indices: Iterable<Int>): List<PlatformInt>
expect fun PlatformIntArray.sliceArray(indices: Collection<Int>): PlatformIntArray
expect fun PlatformIntArray.sliceArray(indices: IntRange): PlatformIntArray
expect fun PlatformIntArray.take(n: Int): List<PlatformInt>
expect fun PlatformIntArray.takeLast(n: Int): List<PlatformInt>
expect inline fun PlatformIntArray.takeLastWhile(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect inline fun PlatformIntArray.takeWhile(predicate: (PlatformInt) -> Boolean): List<PlatformInt>
expect fun PlatformIntArray.reverse()
expect fun PlatformIntArray.reverse(fromIndex: Int, toIndex: Int)
expect fun PlatformIntArray.reversed(): List<PlatformInt>
expect fun PlatformIntArray.reversedArray(): PlatformIntArray
expect fun PlatformIntArray.shuffle()
expect fun PlatformIntArray.shuffle(random: Random)
expect fun PlatformIntArray.sortDescending()
expect fun PlatformIntArray.sorted(): List<PlatformInt>
expect fun PlatformIntArray.sortedArray(): PlatformIntArray
expect fun PlatformIntArray.sortedArrayDescending(): PlatformIntArray
expect inline fun <R : Comparable<R>> PlatformIntArray.sortedBy(crossinline selector: (PlatformInt) -> R?): List<PlatformInt>
expect inline fun <R : Comparable<R>> PlatformIntArray.sortedByDescending(crossinline selector: (PlatformInt) -> R?): List<PlatformInt>
expect fun PlatformIntArray.sortedDescending(): List<PlatformInt>
expect fun PlatformIntArray.sortedWith(comparator: Comparator<in PlatformInt>): List<PlatformInt>
expect fun PlatformIntArray.asList(): List<PlatformInt>
expect infix fun PlatformIntArray.contentEquals(other: PlatformIntArray): Boolean
expect fun PlatformIntArray.contentHashCode(): Int
expect fun PlatformIntArray.contentToString(): String
expect fun PlatformIntArray.copyInto(
    destination: PlatformIntArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = size
): PlatformIntArray
expect fun PlatformIntArray.copyOf(): PlatformIntArray
expect fun PlatformIntArray.copyOf(newSize: Int): PlatformIntArray
expect fun PlatformIntArray.copyOfRange(fromIndex: Int, toIndex: Int): PlatformIntArray
expect fun PlatformIntArray.fill(element: PlatformInt, fromIndex: Int = 0, toIndex: Int = size)
expect val PlatformIntArray.indices: IntRange
expect inline fun PlatformIntArray.isEmpty(): Boolean
expect inline fun PlatformIntArray.isNotEmpty(): Boolean
expect val PlatformIntArray.lastIndex: Int
expect operator fun PlatformIntArray.plus(element: PlatformInt): PlatformIntArray
expect operator fun PlatformIntArray.plus(elements: Collection<PlatformInt>): PlatformIntArray
expect operator fun PlatformIntArray.plus(elements: PlatformIntArray): PlatformIntArray
expect fun PlatformIntArray.sort()
expect fun PlatformIntArray.sort(fromIndex: Int = 0, toIndex: Int = size)
expect fun PlatformIntArray.sortDescending(fromIndex: Int, toIndex: Int)
expect fun PlatformIntArray.toTypedArray(): Array<PlatformInt>
expect inline fun <K, V> PlatformIntArray.associate(transform: (PlatformInt) -> Pair<K, V>): Map<K, V>
expect inline fun <K> PlatformIntArray.associateBy(keySelector: (PlatformInt) -> K): Map<K, PlatformInt>
expect inline fun <K, V> PlatformIntArray.associateBy(
    keySelector: (PlatformInt) -> K,
    valueTransform: (PlatformInt) -> V
): Map<K, V>
expect inline fun <K, M : MutableMap<in K, in PlatformInt>> PlatformIntArray.associateByTo(
    destination: M,
    keySelector: (PlatformInt) -> K
): M
expect inline fun <K, V, M : MutableMap<in K, in V>> PlatformIntArray.associateByTo(
    destination: M,
    keySelector: (PlatformInt) -> K,
    valueTransform: (PlatformInt) -> V
): M
expect inline fun <K, V, M : MutableMap<in K, in V>> PlatformIntArray.associateTo(
    destination: M,
    transform: (PlatformInt) -> Pair<K, V>
): M
expect inline fun <V> PlatformIntArray.associateWith(valueSelector: (PlatformInt) -> V): Map<PlatformInt, V>
expect inline fun <V, M : MutableMap<in PlatformInt, in V>> PlatformIntArray.associateWithTo(
    destination: M,
    valueSelector: (PlatformInt) -> V
): M
expect fun <C : MutableCollection<in PlatformInt>> PlatformIntArray.toCollection(destination: C): C
expect fun PlatformIntArray.toHashSet(): HashSet<PlatformInt>
expect fun PlatformIntArray.toList(): List<PlatformInt>
expect fun PlatformIntArray.toMutableList(): MutableList<PlatformInt>
expect fun PlatformIntArray.toSet(): Set<PlatformInt>
expect inline fun <R> PlatformIntArray.flatMap(transform: (PlatformInt) -> Iterable<R>): List<R>
expect inline fun <R> PlatformIntArray.flatMapIndexed(transform: (index: Int, PlatformInt) -> Iterable<R>): List<R>
expect inline fun <R, C : MutableCollection<in R>> PlatformIntArray.flatMapIndexedTo(
    destination: C,
    transform: (index: Int, PlatformInt) -> Iterable<R>
): C
expect inline fun <R, C : MutableCollection<in R>> PlatformIntArray.flatMapTo(
    destination: C,
    transform: (PlatformInt) -> Iterable<R>
): C
expect inline fun <K> PlatformIntArray.groupBy(keySelector: (PlatformInt) -> K): Map<K, List<PlatformInt>>
expect inline fun <K, V> PlatformIntArray.groupBy(
    keySelector: (PlatformInt) -> K,
    valueTransform: (PlatformInt) -> V
): Map<K, List<V>>
expect inline fun <K, M : MutableMap<in K, MutableList<PlatformInt>>> PlatformIntArray.groupByTo(
    destination: M,
    keySelector: (PlatformInt) -> K
): M
expect inline fun <K, V, M : MutableMap<in K, MutableList<V>>> PlatformIntArray.groupByTo(
    destination: M,
    keySelector: (PlatformInt) -> K,
    valueTransform: (PlatformInt) -> V
): M
expect inline fun <R> PlatformIntArray.map(transform: (PlatformInt) -> R): List<R>
expect inline fun <R> PlatformIntArray.mapIndexed(transform: (index: Int, PlatformInt) -> R): List<R>
expect inline fun <R, C : MutableCollection<in R>> PlatformIntArray.mapIndexedTo(
    destination: C,
    transform: (index: Int, PlatformInt) -> R
): C
expect inline fun <R, C : MutableCollection<in R>> PlatformIntArray.mapTo(
    destination: C,
    transform: (PlatformInt) -> R
): C
expect fun PlatformIntArray.withIndex(): Iterable<IndexedValue<PlatformInt>>
expect fun PlatformIntArray.distinct(): List<PlatformInt>
expect inline fun <K> PlatformIntArray.distinctBy(selector: (PlatformInt) -> K): List<PlatformInt>
expect infix fun PlatformIntArray.intersect(other: Iterable<PlatformInt>): Set<PlatformInt>
expect infix fun PlatformIntArray.subtract(other: Iterable<PlatformInt>): Set<PlatformInt>
expect fun PlatformIntArray.toMutableSet(): MutableSet<PlatformInt>
expect infix fun PlatformIntArray.union(other: Iterable<PlatformInt>): Set<PlatformInt>
expect inline fun PlatformIntArray.all(predicate: (PlatformInt) -> Boolean): Boolean
expect fun PlatformIntArray.any(): Boolean
expect inline fun PlatformIntArray.any(predicate: (PlatformInt) -> Boolean): Boolean
expect inline fun PlatformIntArray.count(): Int
expect inline fun PlatformIntArray.count(predicate: (PlatformInt) -> Boolean): Int
expect inline fun <R> PlatformIntArray.fold(initial: R, operation: (acc: R, PlatformInt) -> R): R
expect inline fun <R> PlatformIntArray.foldIndexed(initial: R, operation: (index: Int, acc: R, PlatformInt) -> R): R
expect inline fun <R> PlatformIntArray.foldRight(initial: R, operation: (PlatformInt, acc: R) -> R): R
expect inline fun <R> PlatformIntArray.foldRightIndexed(
    initial: R,
    operation: (index: Int, PlatformInt, acc: R) -> R
): R
expect inline fun PlatformIntArray.forEach(action: (PlatformInt) -> Unit)
expect inline fun PlatformIntArray.forEachIndexed(action: (index: Int, PlatformInt) -> Unit)
@Deprecated("Use maxOrNull instead.", ReplaceWith("this.maxOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformIntArray.max(): PlatformInt?
@Deprecated("Use maxByOrNull instead.", ReplaceWith("this.maxByOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect inline fun <R : Comparable<R>> PlatformIntArray.maxBy(selector: (PlatformInt) -> R): PlatformInt?
expect inline fun <R : Comparable<R>> PlatformIntArray.maxByOrNull(selector: (PlatformInt) -> R): PlatformInt?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.maxOf(selector: (PlatformInt) -> Double): Double
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.maxOf(selector: (PlatformInt) -> Float): Float
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformIntArray.maxOf(selector: (PlatformInt) -> R): R
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.maxOfOrNull(selector: (PlatformInt) -> Double): Double?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.maxOfOrNull(selector: (PlatformInt) -> Float): Float?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformIntArray.maxOfOrNull(selector: (PlatformInt) -> R): R?
expect inline fun <R> PlatformIntArray.maxOfWith(comparator: Comparator<in R>, selector: (PlatformInt) -> R): R
expect inline fun <R> PlatformIntArray.maxOfWithOrNull(comparator: Comparator<in R>, selector: (PlatformInt) -> R): R?
expect fun PlatformIntArray.maxOrNull(): PlatformInt?
@Deprecated("Use maxWithOrNull instead.", ReplaceWith("this.maxWithOrNull(comparator)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformIntArray.maxWith(comparator: Comparator<in PlatformInt>): PlatformInt?
expect fun PlatformIntArray.maxWithOrNull(comparator: Comparator<in PlatformInt>): PlatformInt?
@Deprecated("Use minOrNull instead.", ReplaceWith("this.minOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformIntArray.min(): PlatformInt?
@Deprecated("Use minByOrNull instead.", ReplaceWith("this.minByOrNull(selector)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect inline fun <R : Comparable<R>> PlatformIntArray.minBy(selector: (PlatformInt) -> R): PlatformInt?
expect inline fun <R : Comparable<R>> PlatformIntArray.minByOrNull(selector: (PlatformInt) -> R): PlatformInt?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.minOf(selector: (PlatformInt) -> Double): Double
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.minOf(selector: (PlatformInt) -> Float): Float
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformIntArray.minOf(selector: (PlatformInt) -> R): R
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.minOfOrNull(selector: (PlatformInt) -> Double): Double?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.minOfOrNull(selector: (PlatformInt) -> Float): Float?
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformIntArray.minOfOrNull(selector: (PlatformInt) -> R): R?
expect inline fun <R> PlatformIntArray.minOfWith(comparator: Comparator<in R>, selector: (PlatformInt) -> R): R
expect inline fun <R> PlatformIntArray.minOfWithOrNull(comparator: Comparator<in R>, selector: (PlatformInt) -> R): R?
expect fun PlatformIntArray.minOrNull(): PlatformInt?
@Deprecated("Use minWithOrNull instead.", ReplaceWith("this.minWithOrNull(comparator)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformIntArray.minWith(comparator: Comparator<in PlatformInt>): PlatformInt?
expect fun PlatformIntArray.minWithOrNull(comparator: Comparator<in PlatformInt>): PlatformInt?
expect fun PlatformIntArray.none(): Boolean
expect inline fun PlatformIntArray.none(predicate: (PlatformInt) -> Boolean): Boolean
expect inline fun PlatformIntArray.onEach(action: (PlatformInt) -> Unit): PlatformIntArray
expect inline fun PlatformIntArray.onEachIndexed(action: (index: Int, PlatformInt) -> Unit): PlatformIntArray
expect inline fun PlatformIntArray.reduce(operation: (acc: PlatformInt, PlatformInt) -> PlatformInt): PlatformInt
expect inline fun PlatformIntArray.reduceIndexed(operation: (index: Int, acc: PlatformInt, PlatformInt) -> PlatformInt): PlatformInt
expect inline fun PlatformIntArray.reduceIndexedOrNull(operation: (index: Int, acc: PlatformInt, PlatformInt) -> PlatformInt): PlatformInt?
expect inline fun PlatformIntArray.reduceOrNull(operation: (acc: PlatformInt, PlatformInt) -> PlatformInt): PlatformInt?
expect inline fun PlatformIntArray.reduceRight(operation: (PlatformInt, acc: PlatformInt) -> PlatformInt): PlatformInt
expect inline fun PlatformIntArray.reduceRightIndexed(operation: (index: Int, PlatformInt, acc: PlatformInt) -> PlatformInt): PlatformInt
expect inline fun PlatformIntArray.reduceRightIndexedOrNull(operation: (index: Int, PlatformInt, acc: PlatformInt) -> PlatformInt): PlatformInt?
expect inline fun PlatformIntArray.reduceRightOrNull(operation: (PlatformInt, acc: PlatformInt) -> PlatformInt): PlatformInt?
expect inline fun <R> PlatformIntArray.runningFold(initial: R, operation: (acc: R, PlatformInt) -> R): List<R>
expect inline fun <R> PlatformIntArray.runningFoldIndexed(
    initial: R,
    operation: (index: Int, acc: R, PlatformInt) -> R
): List<R>
expect inline fun PlatformIntArray.runningReduce(operation: (acc: PlatformInt, PlatformInt) -> PlatformInt): List<PlatformInt>
expect inline fun PlatformIntArray.runningReduceIndexed(operation: (index: Int, acc: PlatformInt, PlatformInt) -> PlatformInt): List<PlatformInt>
expect inline fun <R> PlatformIntArray.scan(initial: R, operation: (acc: R, PlatformInt) -> R): List<R>
expect inline fun <R> PlatformIntArray.scanIndexed(
    initial: R,
    operation: (index: Int, acc: R, PlatformInt) -> R
): List<R>
expect inline fun PlatformIntArray.sumBy(selector: (PlatformInt) -> Int): Int
expect inline fun PlatformIntArray.sumByDouble(selector: (PlatformInt) -> Double): Double
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.sumOf(selector: (PlatformInt) -> Double): Double
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.sumOf(selector: (PlatformInt) -> Int): Int
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.sumOf(selector: (PlatformInt) -> Long): PlatformInt
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.sumOf(selector: (PlatformInt) -> UInt): UInt
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformIntArray.sumOf(selector: (PlatformInt) -> ULong): PlatformUInt
expect inline fun PlatformIntArray.partition(predicate: (PlatformInt) -> Boolean): Pair<List<PlatformInt>, List<PlatformInt>>
expect infix fun <R> PlatformIntArray.zip(other: Array<out R>): List<Pair<PlatformInt, R>>
expect inline fun <R, V> PlatformIntArray.zip(other: Array<out R>, transform: (a: PlatformInt, b: R) -> V): List<V>
expect infix fun <R> PlatformIntArray.zip(other: Iterable<R>): List<Pair<PlatformInt, R>>
expect inline fun <R, V> PlatformIntArray.zip(other: Iterable<R>, transform: (a: PlatformInt, b: R) -> V): List<V>
expect infix fun PlatformIntArray.zip(other: PlatformIntArray): List<Pair<PlatformInt, PlatformInt>>
expect inline fun <V> PlatformIntArray.zip(
    other: PlatformIntArray,
    transform: (a: PlatformInt, b: PlatformInt) -> V
): List<V>
expect fun <A : Appendable> PlatformIntArray.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((PlatformInt) -> CharSequence)? = null
): A
expect fun PlatformIntArray.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((PlatformInt) -> CharSequence)? = null
): String
expect fun PlatformIntArray.asIterable(): Iterable<PlatformInt>
expect fun PlatformIntArray.asSequence(): Sequence<PlatformInt>
expect fun PlatformIntArray.average(): Double
expect fun PlatformIntArray.sum(): PlatformInt
