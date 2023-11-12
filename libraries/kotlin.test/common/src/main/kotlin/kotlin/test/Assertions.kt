/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * A number of helper methods for writing unit tests.
 */
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kotlin.test

import kotlin.contracts.*
import kotlin.internal.*
import kotlin.jvm.JvmName
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Current adapter providing assertion implementations
 */
public val asserter: Asserter
    get() = _asserter ?: lookupAsserter()

/** Used to override current asserter internally */
@ThreadLocal
internal var _asserter: Asserter? = null

/** Asserts that the given [block] returns `true`. */
@JvmName("assertTrueInline")
@InlineOnly
public inline fun assertTrue(message: String? = null, block: () -> Boolean) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    assertTrue(block(), message)
}

/** Asserts that the expression is `true` with an optional [message]. */
public fun assertTrue(actual: Boolean, message: String? = null) {
    contract { returns() implies actual }
    return asserter.assertTrue(message ?: "Expected value to be true.", actual)
}

/** Asserts that the given [block] returns `false`. */
@JvmName("assertFalseInline")
@InlineOnly
public inline fun assertFalse(message: String? = null, block: () -> Boolean) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    assertFalse(block(), message)
}

/** Asserts that the expression is `false` with an optional [message]. */
public fun assertFalse(actual: Boolean, message: String? = null) {
    contract { returns() implies (!actual) }
    return asserter.assertTrue(message ?: "Expected value to be false.", !actual)
}

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
public fun <@OnlyInputTypes T> assertEquals(expected: T, actual: T, message: String? = null) {
    asserter.assertEquals(message, expected, actual)
}

/** Asserts that the difference between the [actual] and the [expected] is within an [absoluteTolerance], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double, message: String? = null) {
    checkDoublesAreEqual(expected, actual, absoluteTolerance, message)
}

/** Asserts that the difference between the [actual] and the [expected] is within an [absoluteTolerance], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float, message: String? = null) {
    checkFloatsAreEqual(expected, actual, absoluteTolerance, message)
}

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
public fun <@OnlyInputTypes T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    asserter.assertNotEquals(message, illegal, actual)
}

/** Asserts that the difference between the [actual] and the [illegal] is not within an [absoluteTolerance], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertNotEquals(illegal: Double, actual: Double, absoluteTolerance: Double, message: String? = null) {
    checkDoublesAreEqual(illegal, actual, absoluteTolerance, message, shouldFail = true)
}

/** Asserts that the difference between the [actual] and the [illegal] is not within an [absoluteTolerance], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertNotEquals(illegal: Float, actual: Float, absoluteTolerance: Float, message: String? = null) {
    checkFloatsAreEqual(illegal, actual, absoluteTolerance, message, shouldFail = true)
}

/** Asserts that [expected] is the same instance as [actual], with an optional [message]. */
public fun <@OnlyInputTypes T> assertSame(expected: T, actual: T, message: String? = null) {
    asserter.assertSame(message, expected, actual)
}

/** Asserts that [actual] is not the same instance as [illegal], with an optional [message]. */
public fun <@OnlyInputTypes T> assertNotSame(illegal: T, actual: T, message: String? = null) {
    asserter.assertNotSame(message, illegal, actual)
}

/**
 * Asserts that [value] is of type [T], with an optional [message].
 *
 * Note that due to type erasure the type check may be partial (e.g. `assertIs<List<String>>(value)`
 * only checks for the class being [List] and not the type of its elements because it's erased).
 */
@SinceKotlin("1.5")
@InlineOnly
@OptIn(ExperimentalStdlibApi::class)
public inline fun <reified T> assertIs(value: Any?, message: String? = null): T {
    contract { returns() implies (value is T) }
    assertIsOfType(value, typeOf<T>(), value is T, message)
    return value as T
}

@PublishedApi
internal fun assertIsOfType(value: Any?, type: KType, result: Boolean, message: String?) {
    asserter.assertTrue({ messagePrefix(message) + "Expected value to be of type <$type>, actual <${value?.let { it::class }}>." }, result)
}

/**
 * Asserts that [value] is not of type [T], with an optional [message].
 *
 * Note that due to type erasure the type check may be partial (e.g. `assertIsNot<List<String>>(value)`
 * only checks for the class being [List] and not the type of its elements because it's erased).
 */
@SinceKotlin("1.5")
@InlineOnly
public inline fun <reified T> assertIsNot(value: Any?, message: String? = null) {
    assertIsNotOfType(value, typeOf<T>(), value !is T, message)
}

@PublishedApi
internal fun assertIsNotOfType(@Suppress("UNUSED_PARAMETER") value: Any?, type: KType, result: Boolean, message: String?) {
    asserter.assertTrue({ messagePrefix(message) + "Expected value to not be of type <$type>." }, result)
}

/** Asserts that the [actual] value is not `null`, with an optional [message]. */
public fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    contract { returns() implies (actual != null) }
    asserter.assertNotNull(message, actual)
    return actual!!
}

/** Asserts that the [actual] value is not `null`, with an optional [message] and a function [block] to process the not-null value. */
@JvmName("assertNotNullInline")
@InlineOnly
public inline fun <T : Any, R> assertNotNull(actual: T?, message: String? = null, block: (T) -> R) {
    contract { returns() implies (actual != null) }
    block(assertNotNull(actual, message))
}

/** Asserts that the [actual] value is `null`, with an optional [message]. */
public fun assertNull(actual: Any?, message: String? = null) {
    asserter.assertNull(message, actual)
}

/** Asserts that the [iterable] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContains(iterable: Iterable<T>, element: T, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the collection to contain the element.\nCollection <$iterable>, element <$element>." },
        iterable.contains(element)
    )
}

/** Asserts that the [sequence] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContains(sequence: Sequence<T>, element: T, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the sequence to contain the element.\nSequence <$sequence>, element <$element>." },
        sequence.contains(element)
    )
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContains(array: Array<T>, element: T, message: String? = null) {
    assertArrayContains(array, element, message, Array<T>::contains, Array<T>::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: ByteArray, element: Byte, message: String? = null) {
    assertArrayContains(array, element, message, ByteArray::contains, ByteArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: ShortArray, element: Short, message: String? = null) {
    assertArrayContains(array, element, message, ShortArray::contains, ShortArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: IntArray, element: Int, message: String? = null) {
    assertArrayContains(array, element, message, IntArray::contains, IntArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: LongArray, element: Long, message: String? = null) {
    assertArrayContains(array, element, message, LongArray::contains, LongArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: BooleanArray, element: Boolean, message: String? = null) {
    assertArrayContains(array, element, message, BooleanArray::contains, BooleanArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(array: CharArray, element: Char, message: String? = null) {
    assertArrayContains(array, element, message, CharArray::contains, CharArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContains(array: UByteArray, element: UByte, message: String? = null) {
    assertArrayContains(array, element, message, UByteArray::contains, UByteArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContains(array: UShortArray, element: UShort, message: String? = null) {
    assertArrayContains(array, element, message, UShortArray::contains, UShortArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContains(array: UIntArray, element: UInt, message: String? = null) {
    assertArrayContains(array, element, message, UIntArray::contains, UIntArray::contentToString)
}

/** Asserts that the [array] contains the specified [element], with an optional [message]. */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContains(array: ULongArray, element: ULong, message: String? = null) {
    assertArrayContains(array, element, message, ULongArray::contains, ULongArray::contentToString)
}

@kotlin.internal.InlineOnly
private inline fun <@OnlyInputTypes A, E> assertArrayContains(
    array: A,
    element: E,
    message: String? = null,
    contains: A.(E) -> Boolean,
    crossinline contentToString: A.() -> String
) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the array to contain the element.\nArray <${array.contentToString()}>, element <${element.toString()}>." }, // Explicitly call toString(): KT-45684
        array.contains(element)
    )
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(range: IntRange, value: Int, message: String? = null) {
    assertRangeContains(range, value, message, IntRange::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(range: LongRange, value: Long, message: String? = null) {
    assertRangeContains(range, value, message, LongRange::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun <T : Comparable<T>> assertContains(range: ClosedRange<T>, value: T, message: String? = null) {
    assertRangeContains(range, value, message, ClosedRange<T>::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.7")
@ExperimentalStdlibApi
public fun <T : Comparable<T>> assertContains(range: OpenEndRange<T>, value: T, message: String? = null) {
    assertRangeContains(range, value, message, OpenEndRange<T>::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(range: CharRange, value: Char, message: String? = null) {
    assertRangeContains(range, value, message, CharRange::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(range: UIntRange, value: UInt, message: String? = null) {
    assertRangeContains(range, value, message, UIntRange::contains)
}

/** Asserts that the [range] contains the specified [value], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(range: ULongRange, value: ULong, message: String? = null) {
    assertRangeContains(range, value, message, ULongRange::contains)
}

@kotlin.internal.InlineOnly
private inline fun <R, V> assertRangeContains(range: R, value: V, message: String? = null, contains: R.(V) -> Boolean) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the range <$range> to contain the value <${value.toString()}>." }, // Explicitly call toString(): KT-45684
        range.contains(value)
    )
}

/** Asserts that the [map] contains the specified [key], with an optional [message]. */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes K, V> assertContains(map: Map<K, V>, key: K, message: String? = null) {
    asserter.assertTrue({ messagePrefix(message) + "Expected the map to contain the key.\nMap <$map>, key <$key>." }, map.containsKey(key))
}

/**
 * Asserts that the [charSequence] contains the specified [char], with an optional [message].
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 */
@SinceKotlin("1.5")
public fun assertContains(charSequence: CharSequence, char: Char, ignoreCase: Boolean = false, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the char sequence to contain the char.\nCharSequence <$charSequence>, char <$char>, ignoreCase <$ignoreCase>." },
        charSequence.contains(char, ignoreCase)
    )
}

/**
 * Asserts that the [charSequence] contains the specified [other] char sequence as a substring, with an optional [message].
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
@SinceKotlin("1.5")
public fun assertContains(charSequence: CharSequence, other: CharSequence, ignoreCase: Boolean = false, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the char sequence to contain the substring.\nCharSequence <$charSequence>, substring <$other>, ignoreCase <$ignoreCase>." },
        charSequence.contains(other, ignoreCase)
    )
}

/** Asserts that the [charSequence] contains at least one match of the specified regular expression [regex], with an optional [message]. */
@SinceKotlin("1.5")
public fun assertContains(charSequence: CharSequence, regex: Regex, message: String? = null) {
    asserter.assertTrue(
        { messagePrefix(message) + "Expected the char sequence to contain the regular expression.\nCharSequence <$charSequence>, regex <$regex>." },
        charSequence.contains(regex)
    )
}

/**
 * Asserts that the [expected] iterable is *structurally* equal to the [actual] iterable,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 *
 * The elements are compared for equality with the [equals][Any.equals] function.
 * For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
 */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContentEquals(expected: Iterable<T>?, actual: Iterable<T>?, message: String? = null) {
    assertIterableContentEquals("Iterable", message, expected, actual, Iterable<*>::iterator)
}


@SinceKotlin("1.5")
@Deprecated("'assertContentEquals' for Set arguments is ambiguous. Use 'assertEquals' to compare content with the unordered set equality, or cast one of arguments to Iterable to compare the set elements in order of iteration.",
            level = DeprecationLevel.ERROR,
            replaceWith = ReplaceWith("assertContentEquals(expected, actual?.asIterable(), message)"))
public fun <@OnlyInputTypes T> assertContentEquals(expected: Set<T>?, actual: Set<T>?, message: String? = null): Unit =
    assertContentEquals(expected, actual?.asIterable(), message)

/**
 * Asserts that the [expected] sequence is *structurally* equal to the [actual] sequence,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 *
 * The elements are compared for equality with the [equals][Any.equals] function.
 * For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
 */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContentEquals(expected: Sequence<T>?, actual: Sequence<T>?, message: String? = null) {
    assertIterableContentEquals("Sequence", message, expected, actual, Sequence<*>::iterator)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 *
 * The elements are compared for equality with the [equals][Any.equals] function.
 * For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
 */
@SinceKotlin("1.5")
public fun <@OnlyInputTypes T> assertContentEquals(expected: Array<T>?, actual: Array<T>?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, Array<*>::get, Array<*>?::contentToString, Array<*>?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: ByteArray?, actual: ByteArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, ByteArray::get, ByteArray?::contentToString, ByteArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: ShortArray?, actual: ShortArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, ShortArray::get, ShortArray?::contentToString, ShortArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: IntArray?, actual: IntArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, IntArray::get, IntArray?::contentToString, IntArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: LongArray?, actual: LongArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, LongArray::get, LongArray?::contentToString, LongArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 *
 * The elements are compared for equality with the [equals][Any.equals] function.
 * For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: FloatArray?, actual: FloatArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, FloatArray::get, FloatArray?::contentToString, FloatArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 *
 * The elements are compared for equality with the [equals][Any.equals] function.
 * For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: DoubleArray?, actual: DoubleArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, DoubleArray::get, DoubleArray?::contentToString, DoubleArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: BooleanArray?, actual: BooleanArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, BooleanArray::get, BooleanArray?::contentToString, BooleanArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
public fun assertContentEquals(expected: CharArray?, actual: CharArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, CharArray::get, CharArray?::contentToString, CharArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContentEquals(expected: UByteArray?, actual: UByteArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, UByteArray::get, UByteArray?::contentToString, UByteArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContentEquals(expected: UShortArray?, actual: UShortArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, UShortArray::get, UShortArray?::contentToString, UShortArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContentEquals(expected: UIntArray?, actual: UIntArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, UIntArray::get, UIntArray?::contentToString, UIntArray?::contentEquals)
}

/**
 * Asserts that the [expected] array is *structurally* equal to the [actual] array,
 * i.e. contains the same number of the same elements in the same order, with an optional [message].
 */
@SinceKotlin("1.5")
@OptIn(ExperimentalUnsignedTypes::class)
public fun assertContentEquals(expected: ULongArray?, actual: ULongArray?, message: String? = null) {
    assertArrayContentEquals(message, expected, actual, { it.size }, ULongArray::get, ULongArray?::contentToString, ULongArray?::contentEquals)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional [message]. */
public fun fail(message: String? = null): Nothing {
    asserter.fail(message)
}

/**
 * Marks a test as having failed if this point in the execution path is reached, with an optional [message]
 * and [cause] exception.
 *
 * The [cause] exception is set as the root cause of the test failure.
 */
@SinceKotlin("1.4")
public fun fail(message: String? = null, cause: Throwable? = null): Nothing {
    asserter.fail(message, cause)
}

/** Asserts that given function [block] returns the given [expected] value. */
@JvmName("expectInline")
@InlineOnly
public inline fun <@OnlyInputTypes T> expect(expected: T, block: () -> T) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    assertEquals(expected, block())
}

/** Asserts that given function [block] returns the given [expected] value and use the given [message] if it fails. */
@JvmName("expectInline")
@InlineOnly
public inline fun <@OnlyInputTypes T> expect(expected: T, message: String?, block: () -> T) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    assertEquals(expected, block(), message)
}

/**
 * Asserts that given function [block] fails by throwing an exception.
 *
 * @return An exception that was expected to be thrown and was successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
@InlineOnly
@JvmName("assertFailsInline")
public inline fun assertFails(block: () -> Unit): Throwable =
    checkResultIsFailure(null, runCatching(block))

/**
 * Asserts that given function [block] fails by throwing an exception.
 *
 * If the assertion fails, the specified [message] is used unless it is null as a prefix for the failure message.
 *
 * @return An exception that was expected to be thrown and was successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
@SinceKotlin("1.1")
@InlineOnly
@JvmName("assertFailsInline")
public inline fun assertFails(message: String?, block: () -> Unit): Throwable =
    checkResultIsFailure(message, runCatching(block))

@PublishedApi
internal fun checkResultIsFailure(message: String?, blockResult: Result<Unit>): Throwable {
    blockResult.fold(
        onSuccess = {
            asserter.fail(messagePrefix(message) + "Expected an exception to be thrown, but was completed successfully.")
        },
        onFailure = { e ->
            return e
        }
    )
}

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *
 * If the assertion fails, the specified [message] is used unless it is null as a prefix for the failure message.
 *
 * @return An exception of the expected exception type [T] that successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
@InlineOnly
public inline fun <reified T : Throwable> assertFailsWith(message: String? = null, block: () -> Unit): T =
    assertFailsWith(T::class, message, block)

/**
 * Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown.
 *
 * @return An exception of the expected exception type [T] that successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
@InlineOnly
@JvmName("assertFailsWithInline")
public inline fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, block: () -> Unit): T = assertFailsWith(exceptionClass, null, block)

/**
 * Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown.
 *
 * If the assertion fails, the specified [message] is used unless it is null as a prefix for the failure message.
 *
 * @return An exception of the expected exception type [T] that successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
@InlineOnly
@JvmName("assertFailsWithInline")
public inline fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T =
    checkResultIsFailure(exceptionClass, message, runCatching(block))

/** Platform-specific construction of AssertionError with cause */
internal expect fun AssertionErrorWithCause(message: String?, cause: Throwable?): AssertionError

/**
 * Abstracts the logic for performing assertions. Specific implementations of [Asserter] can use JUnit
 * or TestNG assertion facilities.
 */
public interface Asserter {
    /**
     * Fails the current test with the specified message.
     *
     * @param message the message to report.
     */
    public fun fail(message: String?): Nothing

    /**
     * Fails the current test with the specified message and cause exception.
     *
     * @param message the message to report.
     * @param cause the exception to set as the root cause of the reported failure.
     */
    @SinceKotlin("1.4")
    public fun fail(message: String?, cause: Throwable?): Nothing

    /**
     * Asserts that the specified value is `true`.
     *
     * @param lazyMessage the function to return a message to report if the assertion fails.
     */
    public fun assertTrue(lazyMessage: () -> String?, actual: Boolean): Unit {
        if (!actual) {
            fail(lazyMessage())
        }
    }

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertTrue(message: String?, actual: Boolean): Unit {
        assertTrue({ message }, actual)
    }

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertEquals(message: String?, expected: Any?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Expected <$expected>, actual <$actual>." }, actual == expected)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotEquals(message: String?, illegal: Any?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Illegal value: <$actual>." }, actual != illegal)
    }

    /**
     * Asserts that the specified values are the same instance.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertSame(message: String?, expected: Any?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Expected <$expected>, actual <$actual> is not same." }, actual === expected)
    }

    /**
     * Asserts that the specified values are not the same instance.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotSame(message: String?, illegal: Any?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Expected not same as <$actual>." }, actual !== illegal)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNull(message: String?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Expected value to be null, but was: <$actual>." }, actual == null)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotNull(message: String?, actual: Any?): Unit {
        assertTrue({ messagePrefix(message) + "Expected value to be not null." }, actual != null)
    }

}

/**
 * Checks applicability and provides Asserter instance
 */
public interface AsserterContributor {
    /**
     * Provides [Asserter] instance or `null` depends on the current context.
     *
     * @return asserter instance or null if it is not applicable now
     */
    public fun contribute(): Asserter?
}

