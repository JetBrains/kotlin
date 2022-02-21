@file:Suppress("NO_ACTUAL_FOR_EXPECT", "unused")

package kotlin.collections

import kotlin.random.Random

expect inline operator fun PlatformUIntArray.component1(): PlatformUInt
expect inline operator fun PlatformUIntArray.component2(): PlatformUInt
expect inline operator fun PlatformUIntArray.component3(): PlatformUInt
expect inline operator fun PlatformUIntArray.component4(): PlatformUInt
expect inline operator fun PlatformUIntArray.component5(): PlatformUInt
expect operator fun PlatformUIntArray.contains(element: PlatformUInt): Boolean
expect fun PlatformUIntArray.elementAt(index: Int): PlatformUInt
expect inline fun PlatformUIntArray.elementAtOrElse(index: Int, defaultValue: (Int) -> PlatformUInt): PlatformUInt
expect inline fun PlatformUIntArray.elementAtOrNull(index: Int): PlatformUInt?
expect inline fun PlatformUIntArray.find(predicate: (PlatformUInt) -> Boolean): PlatformUInt?
expect inline fun PlatformUIntArray.findLast(predicate: (PlatformUInt) -> Boolean): PlatformUInt?
expect fun PlatformUIntArray.first(): PlatformUInt
expect inline fun PlatformUIntArray.first(predicate: (PlatformUInt) -> Boolean): PlatformUInt
expect fun PlatformUIntArray.firstOrNull(): PlatformUInt?
expect inline fun PlatformUIntArray.firstOrNull(predicate: (PlatformUInt) -> Boolean): PlatformUInt?
expect inline fun PlatformUIntArray.getOrElse(index: Int, defaultValue: (Int) -> PlatformUInt): PlatformUInt
expect fun PlatformUIntArray.getOrNull(index: Int): PlatformUInt?
expect fun PlatformUIntArray.indexOf(element: PlatformUInt): Int
expect inline fun PlatformUIntArray.indexOfFirst(predicate: (PlatformUInt) -> Boolean): Int
expect inline fun PlatformUIntArray.indexOfLast(predicate: (PlatformUInt) -> Boolean): Int
expect fun PlatformUIntArray.last(): PlatformUInt
expect inline fun PlatformUIntArray.last(predicate: (PlatformUInt) -> Boolean): PlatformUInt
expect fun PlatformUIntArray.lastIndexOf(element: PlatformUInt): Int
expect fun PlatformUIntArray.lastOrNull(): PlatformUInt?
expect inline fun PlatformUIntArray.lastOrNull(predicate: (PlatformUInt) -> Boolean): PlatformUInt?
expect inline fun PlatformUIntArray.random(): PlatformUInt
expect fun PlatformUIntArray.random(random: Random): PlatformUInt
expect inline fun PlatformUIntArray.randomOrNull(): PlatformUInt?
expect fun PlatformUIntArray.randomOrNull(random: Random): PlatformUInt?
expect fun PlatformUIntArray.single(): PlatformUInt
expect inline fun PlatformUIntArray.single(predicate: (PlatformUInt) -> Boolean): PlatformUInt
expect fun PlatformUIntArray.singleOrNull(): PlatformUInt?
expect inline fun PlatformUIntArray.singleOrNull(predicate: (PlatformUInt) -> Boolean): PlatformUInt?
expect fun PlatformUIntArray.drop(n: Int): List<PlatformUInt>
expect fun PlatformUIntArray.dropLast(n: Int): List<PlatformUInt>
expect inline fun PlatformUIntArray.dropLastWhile(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun PlatformUIntArray.dropWhile(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun PlatformUIntArray.filter(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun PlatformUIntArray.filterIndexed(predicate: (index: Int, PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun <C : MutableCollection<in PlatformUInt>> PlatformUIntArray.filterIndexedTo(
    destination: C,
    predicate: (index: Int, PlatformUInt) -> Boolean
): C

expect inline fun PlatformUIntArray.filterNot(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun <C : MutableCollection<in PlatformUInt>> PlatformUIntArray.filterNotTo(
    destination: C,
    predicate: (PlatformUInt) -> Boolean
): C

expect inline fun <C : MutableCollection<in PlatformUInt>> PlatformUIntArray.filterTo(
    destination: C,
    predicate: (PlatformUInt) -> Boolean
): C

expect fun PlatformUIntArray.slice(indices: IntRange): List<PlatformUInt>
expect fun PlatformUIntArray.slice(indices: Iterable<Int>): List<PlatformUInt>
expect fun PlatformUIntArray.sliceArray(indices: Collection<Int>): PlatformUIntArray
expect fun PlatformUIntArray.sliceArray(indices: IntRange): PlatformUIntArray
expect fun PlatformUIntArray.take(n: Int): List<PlatformUInt>
expect fun PlatformUIntArray.takeLast(n: Int): List<PlatformUInt>
expect inline fun PlatformUIntArray.takeLastWhile(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect inline fun PlatformUIntArray.takeWhile(predicate: (PlatformUInt) -> Boolean): List<PlatformUInt>
expect fun PlatformUIntArray.reverse()
expect fun PlatformUIntArray.reverse(fromIndex: Int, toIndex: Int)
expect fun PlatformUIntArray.reversed(): List<PlatformUInt>
expect fun PlatformUIntArray.reversedArray(): PlatformUIntArray
expect fun PlatformUIntArray.shuffle()
expect fun PlatformUIntArray.shuffle(random: Random)
expect fun PlatformUIntArray.sortDescending()
expect fun PlatformUIntArray.sorted(): List<PlatformUInt>
expect fun PlatformUIntArray.sortedArray(): PlatformUIntArray
expect fun PlatformUIntArray.sortedArrayDescending(): PlatformUIntArray
expect inline fun <R : Comparable<R>> PlatformUIntArray.sortedBy(crossinline selector: (PlatformUInt) -> R?): List<PlatformUInt>
expect inline fun <R : Comparable<R>> PlatformUIntArray.sortedByDescending(crossinline selector: (PlatformUInt) -> R?): List<PlatformUInt>
expect fun PlatformUIntArray.sortedDescending(): List<PlatformUInt>
expect fun PlatformUIntArray.sortedWith(comparator: Comparator<in PlatformUInt>): List<PlatformUInt>
expect fun PlatformUIntArray.asList(): List<PlatformUInt>
expect infix fun PlatformUIntArray.contentEquals(other: PlatformUIntArray): Boolean
expect fun PlatformUIntArray.contentHashCode(): Int
expect fun PlatformUIntArray.contentToString(): String
expect fun PlatformUIntArray.copyInto(
    destination: PlatformUIntArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = size
): PlatformUIntArray

expect fun PlatformUIntArray.copyOf(): PlatformUIntArray
expect fun PlatformUIntArray.copyOf(newSize: Int): PlatformUIntArray
expect fun PlatformUIntArray.copyOfRange(fromIndex: Int, toIndex: Int): PlatformUIntArray
expect fun PlatformUIntArray.fill(element: PlatformUInt, fromIndex: Int = 0, toIndex: Int = size)
expect val PlatformUIntArray.indices: IntRange
expect inline fun PlatformUIntArray.isEmpty(): Boolean
expect inline fun PlatformUIntArray.isNotEmpty(): Boolean
expect val PlatformUIntArray.lastIndex: Int
expect operator fun PlatformUIntArray.plus(element: PlatformUInt): PlatformUIntArray
expect operator fun PlatformUIntArray.plus(elements: Collection<PlatformUInt>): PlatformUIntArray
expect operator fun PlatformUIntArray.plus(elements: PlatformUIntArray): PlatformUIntArray
expect fun PlatformUIntArray.sort()
expect fun PlatformUIntArray.sort(fromIndex: Int = 0, toIndex: Int = size)
expect fun PlatformUIntArray.sortDescending(fromIndex: Int, toIndex: Int)
expect fun PlatformUIntArray.toTypedArray(): Array<PlatformUInt>
expect inline fun <K, V> PlatformUIntArray.associate(transform: (PlatformUInt) -> Pair<K, V>): Map<K, V>
expect inline fun <K> PlatformUIntArray.associateBy(keySelector: (PlatformUInt) -> K): Map<K, PlatformUInt>
expect inline fun <K, V> PlatformUIntArray.associateBy(
    keySelector: (PlatformUInt) -> K,
    valueTransform: (PlatformUInt) -> V
): Map<K, V>

expect inline fun <K, M : MutableMap<in K, in PlatformUInt>> PlatformUIntArray.associateByTo(
    destination: M,
    keySelector: (PlatformUInt) -> K
): M

expect inline fun <K, V, M : MutableMap<in K, in V>> PlatformUIntArray.associateByTo(
    destination: M,
    keySelector: (PlatformUInt) -> K,
    valueTransform: (PlatformUInt) -> V
): M

expect inline fun <K, V, M : MutableMap<in K, in V>> PlatformUIntArray.associateTo(
    destination: M,
    transform: (PlatformUInt) -> Pair<K, V>
): M

expect inline fun <V> PlatformUIntArray.associateWith(valueSelector: (PlatformUInt) -> V): Map<PlatformUInt, V>
expect inline fun <V, M : MutableMap<in PlatformUInt, in V>> PlatformUIntArray.associateWithTo(
    destination: M,
    valueSelector: (PlatformUInt) -> V
): M

expect fun <C : MutableCollection<in PlatformUInt>> PlatformUIntArray.toCollection(destination: C): C
expect fun PlatformUIntArray.toHashSet(): HashSet<PlatformUInt>
expect fun PlatformUIntArray.toList(): List<PlatformUInt>
expect fun PlatformUIntArray.toMutableList(): MutableList<PlatformUInt>
expect fun PlatformUIntArray.toSet(): Set<PlatformUInt>
expect inline fun <R> PlatformUIntArray.flatMap(transform: (PlatformUInt) -> Iterable<R>): List<R>
expect inline fun <R> PlatformUIntArray.flatMapIndexed(transform: (index: Int, PlatformUInt) -> Iterable<R>): List<R>
expect inline fun <R, C : MutableCollection<in R>> PlatformUIntArray.flatMapIndexedTo(
    destination: C,
    transform: (index: Int, PlatformUInt) -> Iterable<R>
): C

expect inline fun <R, C : MutableCollection<in R>> PlatformUIntArray.flatMapTo(
    destination: C,
    transform: (PlatformUInt) -> Iterable<R>
): C

expect inline fun <K> PlatformUIntArray.groupBy(keySelector: (PlatformUInt) -> K): Map<K, List<PlatformUInt>>
expect inline fun <K, V> PlatformUIntArray.groupBy(
    keySelector: (PlatformUInt) -> K,
    valueTransform: (PlatformUInt) -> V
): Map<K, List<V>>

expect inline fun <K, M : MutableMap<in K, MutableList<PlatformUInt>>> PlatformUIntArray.groupByTo(
    destination: M,
    keySelector: (PlatformUInt) -> K
): M

expect inline fun <K, V, M : MutableMap<in K, MutableList<V>>> PlatformUIntArray.groupByTo(
    destination: M,
    keySelector: (PlatformUInt) -> K,
    valueTransform: (PlatformUInt) -> V
): M

expect inline fun <R> PlatformUIntArray.map(transform: (PlatformUInt) -> R): List<R>
expect inline fun <R> PlatformUIntArray.mapIndexed(transform: (index: Int, PlatformUInt) -> R): List<R>
expect inline fun <R, C : MutableCollection<in R>> PlatformUIntArray.mapIndexedTo(
    destination: C,
    transform: (index: Int, PlatformUInt) -> R
): C

expect inline fun <R, C : MutableCollection<in R>> PlatformUIntArray.mapTo(
    destination: C,
    transform: (PlatformUInt) -> R
): C

expect fun PlatformUIntArray.withIndex(): Iterable<IndexedValue<PlatformUInt>>
expect fun PlatformUIntArray.distinct(): List<PlatformUInt>
expect inline fun <K> PlatformUIntArray.distinctBy(selector: (PlatformUInt) -> K): List<PlatformUInt>
expect infix fun PlatformUIntArray.intersect(other: Iterable<PlatformUInt>): Set<PlatformUInt>
expect infix fun PlatformUIntArray.subtract(other: Iterable<PlatformUInt>): Set<PlatformUInt>
expect fun PlatformUIntArray.toMutableSet(): MutableSet<PlatformUInt>
expect infix fun PlatformUIntArray.union(other: Iterable<PlatformUInt>): Set<PlatformUInt>
expect inline fun PlatformUIntArray.all(predicate: (PlatformUInt) -> Boolean): Boolean
expect fun PlatformUIntArray.any(): Boolean
expect inline fun PlatformUIntArray.any(predicate: (PlatformUInt) -> Boolean): Boolean
expect inline fun PlatformUIntArray.count(): Int
expect inline fun PlatformUIntArray.count(predicate: (PlatformUInt) -> Boolean): Int
expect inline fun <R> PlatformUIntArray.fold(initial: R, operation: (acc: R, PlatformUInt) -> R): R
expect inline fun <R> PlatformUIntArray.foldIndexed(initial: R, operation: (index: Int, acc: R, PlatformUInt) -> R): R
expect inline fun <R> PlatformUIntArray.foldRight(initial: R, operation: (PlatformUInt, acc: R) -> R): R
expect inline fun <R> PlatformUIntArray.foldRightIndexed(
    initial: R,
    operation: (index: Int, PlatformUInt, acc: R) -> R
): R

expect inline fun PlatformUIntArray.forEach(action: (PlatformUInt) -> Unit)
expect inline fun PlatformUIntArray.forEachIndexed(action: (index: Int, PlatformUInt) -> Unit)

@Deprecated("Use maxOrNull instead.", ReplaceWith("this.maxOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformUIntArray.max(): PlatformUInt?

@Deprecated("Use maxByOrNull instead.", ReplaceWith("this.maxByOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect inline fun <R : Comparable<R>> PlatformUIntArray.maxBy(selector: (PlatformUInt) -> R): PlatformUInt?
expect inline fun <R : Comparable<R>> PlatformUIntArray.maxByOrNull(selector: (PlatformUInt) -> R): PlatformUInt?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.maxOf(selector: (PlatformUInt) -> Double): Double

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.maxOf(selector: (PlatformUInt) -> Float): Float

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformUIntArray.maxOf(selector: (PlatformUInt) -> R): R

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.maxOfOrNull(selector: (PlatformUInt) -> Double): Double?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.maxOfOrNull(selector: (PlatformUInt) -> Float): Float?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformUIntArray.maxOfOrNull(selector: (PlatformUInt) -> R): R?
expect inline fun <R> PlatformUIntArray.maxOfWith(comparator: Comparator<in R>, selector: (PlatformUInt) -> R): R
expect inline fun <R> PlatformUIntArray.maxOfWithOrNull(comparator: Comparator<in R>, selector: (PlatformUInt) -> R): R?
expect fun PlatformUIntArray.maxOrNull(): PlatformUInt?

@Deprecated("Use maxWithOrNull instead.", ReplaceWith("this.maxWithOrNull(comparator)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformUIntArray.maxWith(comparator: Comparator<in PlatformUInt>): PlatformUInt?
expect fun PlatformUIntArray.maxWithOrNull(comparator: Comparator<in PlatformUInt>): PlatformUInt?

@Deprecated("Use minOrNull instead.", ReplaceWith("this.minOrNull()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformUIntArray.min(): PlatformUInt?

@Deprecated("Use minByOrNull instead.", ReplaceWith("this.minByOrNull(selector)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect inline fun <R : Comparable<R>> PlatformUIntArray.minBy(selector: (PlatformUInt) -> R): PlatformUInt?
expect inline fun <R : Comparable<R>> PlatformUIntArray.minByOrNull(selector: (PlatformUInt) -> R): PlatformUInt?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.minOf(selector: (PlatformUInt) -> Double): Double

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.minOf(selector: (PlatformUInt) -> Float): Float

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformUIntArray.minOf(selector: (PlatformUInt) -> R): R

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.minOfOrNull(selector: (PlatformUInt) -> Double): Double?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.minOfOrNull(selector: (PlatformUInt) -> Float): Float?

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun <R : Comparable<R>> PlatformUIntArray.minOfOrNull(selector: (PlatformUInt) -> R): R?
expect inline fun <R> PlatformUIntArray.minOfWith(comparator: Comparator<in R>, selector: (PlatformUInt) -> R): R
expect inline fun <R> PlatformUIntArray.minOfWithOrNull(comparator: Comparator<in R>, selector: (PlatformUInt) -> R): R?
expect fun PlatformUIntArray.minOrNull(): PlatformUInt?

@Deprecated("Use minWithOrNull instead.", ReplaceWith("this.minWithOrNull(comparator)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
expect fun PlatformUIntArray.minWith(comparator: Comparator<in PlatformUInt>): PlatformUInt?
expect fun PlatformUIntArray.minWithOrNull(comparator: Comparator<in PlatformUInt>): PlatformUInt?
expect fun PlatformUIntArray.none(): Boolean
expect inline fun PlatformUIntArray.none(predicate: (PlatformUInt) -> Boolean): Boolean
expect inline fun PlatformUIntArray.onEach(action: (PlatformUInt) -> Unit): PlatformUIntArray
expect inline fun PlatformUIntArray.onEachIndexed(action: (index: Int, PlatformUInt) -> Unit): PlatformUIntArray
expect inline fun PlatformUIntArray.reduce(operation: (acc: PlatformUInt, PlatformUInt) -> PlatformUInt): PlatformUInt
expect inline fun PlatformUIntArray.reduceIndexed(operation: (index: Int, acc: PlatformUInt, PlatformUInt) -> PlatformUInt): PlatformUInt
expect inline fun PlatformUIntArray.reduceIndexedOrNull(operation: (index: Int, acc: PlatformUInt, PlatformUInt) -> PlatformUInt): PlatformUInt?
expect inline fun PlatformUIntArray.reduceOrNull(operation: (acc: PlatformUInt, PlatformUInt) -> PlatformUInt): PlatformUInt?
expect inline fun PlatformUIntArray.reduceRight(operation: (PlatformUInt, acc: PlatformUInt) -> PlatformUInt): PlatformUInt
expect inline fun PlatformUIntArray.reduceRightIndexed(operation: (index: Int, PlatformUInt, acc: PlatformUInt) -> PlatformUInt): PlatformUInt
expect inline fun PlatformUIntArray.reduceRightIndexedOrNull(operation: (index: Int, PlatformUInt, acc: PlatformUInt) -> PlatformUInt): PlatformUInt?
expect inline fun PlatformUIntArray.reduceRightOrNull(operation: (PlatformUInt, acc: PlatformUInt) -> PlatformUInt): PlatformUInt?
expect inline fun <R> PlatformUIntArray.runningFold(initial: R, operation: (acc: R, PlatformUInt) -> R): List<R>
expect inline fun <R> PlatformUIntArray.runningFoldIndexed(
    initial: R,
    operation: (index: Int, acc: R, PlatformUInt) -> R
): List<R>

expect inline fun PlatformUIntArray.runningReduce(operation: (acc: PlatformUInt, PlatformUInt) -> PlatformUInt): List<PlatformUInt>
expect inline fun PlatformUIntArray.runningReduceIndexed(operation: (index: Int, acc: PlatformUInt, PlatformUInt) -> PlatformUInt): List<PlatformUInt>
expect inline fun <R> PlatformUIntArray.scan(initial: R, operation: (acc: R, PlatformUInt) -> R): List<R>
expect inline fun <R> PlatformUIntArray.scanIndexed(
    initial: R,
    operation: (index: Int, acc: R, PlatformUInt) -> R
): List<R>

expect inline fun PlatformUIntArray.sumBy(selector: (PlatformUInt) -> Int): Int
expect inline fun PlatformUIntArray.sumByDouble(selector: (PlatformUInt) -> Double): Double

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.sumOf(selector: (PlatformUInt) -> Double): Double

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.sumOf(selector: (PlatformUInt) -> Int): Int

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.sumOf(selector: (PlatformUInt) -> Long): PlatformUInt

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.sumOf(selector: (PlatformUInt) -> UInt): UInt

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
expect inline fun PlatformUIntArray.sumOf(selector: (PlatformUInt) -> ULong): PlatformUInt
expect inline fun PlatformUIntArray.partition(predicate: (PlatformUInt) -> Boolean): Pair<List<PlatformUInt>, List<PlatformUInt>>
expect infix fun <R> PlatformUIntArray.zip(other: Array<out R>): List<Pair<PlatformUInt, R>>
expect inline fun <R, V> PlatformUIntArray.zip(other: Array<out R>, transform: (a: PlatformUInt, b: R) -> V): List<V>
expect infix fun <R> PlatformUIntArray.zip(other: Iterable<R>): List<Pair<PlatformUInt, R>>
expect inline fun <R, V> PlatformUIntArray.zip(other: Iterable<R>, transform: (a: PlatformUInt, b: R) -> V): List<V>
expect infix fun PlatformUIntArray.zip(other: PlatformUIntArray): List<Pair<PlatformUInt, PlatformUInt>>
expect inline fun <V> PlatformUIntArray.zip(
    other: PlatformUIntArray,
    transform: (a: PlatformUInt, b: PlatformUInt) -> V
): List<V>

expect fun <A : Appendable> PlatformUIntArray.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((PlatformUInt) -> CharSequence)? = null
): A

expect fun PlatformUIntArray.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((PlatformUInt) -> CharSequence)? = null
): String

expect fun PlatformUIntArray.asIterable(): Iterable<PlatformUInt>
expect fun PlatformUIntArray.asSequence(): Sequence<PlatformUInt>

// doesn't exist for unsigned
// expect fun PlatformUIntArray.average(): Double
expect fun PlatformUIntArray.sum(): PlatformUInt
