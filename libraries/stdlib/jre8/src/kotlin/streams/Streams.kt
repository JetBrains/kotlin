@file:JvmName("StreamsKt")
package kotlin.streams

import java.util.*
import java.util.stream.*

/**
 * Creates a [Sequence] instance that wraps the original stream iterating through its elements.
 */
public fun <T> Stream<T>.asSequence(): Sequence<T> = Sequence { iterator() }

/**
 * Creates a [Sequence] instance that wraps the original stream iterating through its elements.
 */
public fun IntStream.asSequence(): Sequence<Int> = Sequence { iterator() }

/**
 * Creates a [Sequence] instance that wraps the original stream iterating through its elements.
 */
public fun LongStream.asSequence(): Sequence<Long> = Sequence { iterator() }

/**
 * Creates a [Sequence] instance that wraps the original stream iterating through its elements.
 */
public fun DoubleStream.asSequence(): Sequence<Double> = Sequence { iterator() }

/**
 * Creates a sequential [Stream] instance that produces elements from the original sequence.
 */
public fun <T> Sequence<T>.asStream(): Stream<T> = StreamSupport.stream({ Spliterators.spliteratorUnknownSize(iterator(), 0) }, 0, false)

/**
 * Returns a [List] containing all elements produced by this stream.
 */
public fun <T> Stream<T>.toList(): List<T> = collect(Collectors.toList<T>())

/**
 * Returns a [List] containing all elements produced by this stream.
 */
public fun IntStream.toList(): List<Int> = toArray().asList()

/**
 * Returns a [List] containing all elements produced by this stream.
 */
public fun LongStream.toList(): List<Long> = toArray().asList()

/**
 * Returns a [List] containing all elements produced by this stream.
 */
public fun DoubleStream.toList(): List<Double> = toArray().asList()

