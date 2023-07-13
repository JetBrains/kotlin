/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")
@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package kotlin.text

import kotlin.contracts.*

/**
 * A mutable sequence of characters.
 *
 * String builder can be used to efficiently perform multiple string manipulation operations.
 */
expect class StringBuilder : Appendable, CharSequence {
    /** Constructs an empty string builder. */
    constructor()

    /** Constructs an empty string builder with the specified initial [capacity]. */
    constructor(capacity: Int)

    /** Constructs a string builder that contains the same characters as the specified [content] char sequence. */
    constructor(content: CharSequence)

    /** Constructs a string builder that contains the same characters as the specified [content] string. */
    @SinceKotlin("1.3")
//    @ExperimentalStdlibApi
    constructor(content: String)

    override val length: Int

    override operator fun get(index: Int): Char

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    override fun append(value: Char): StringBuilder
    override fun append(value: CharSequence?): StringBuilder
    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder

    /**
     * Reverses the contents of this string builder and returns this instance.
     *
     * Surrogate pairs included in this string builder are treated as single characters.
     * Therefore, the order of the high-low surrogates is never reversed.
     *
     * Note that the reverse operation may produce new surrogate pairs that were unpaired low-surrogates and high-surrogates before the operation.
     * For example, reversing `"\uDC00\uD800"` produces `"\uD800\uDC00"` which is a valid surrogate pair.
     */
    fun reverse(): StringBuilder

    /**
     * Appends the string representation of the specified object [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    fun append(value: Any?): StringBuilder

    /**
     * Appends the string representation of the specified boolean [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.3")
    fun append(value: Boolean): StringBuilder

    /**
     * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Byte): StringBuilder

    /**
     * Appends the string representation of the specified short [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Short): StringBuilder

    /**
     * Appends the string representation of the specified int [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Int): StringBuilder

    /**
     * Appends the string representation of the specified long [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Long): StringBuilder

    /**
     * Appends the string representation of the specified float [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Float): StringBuilder

    /**
     * Appends the string representation of the specified double [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @SinceKotlin("1.9")
    fun append(value: Double): StringBuilder

    /**
     * Appends characters in the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at the index 0.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun append(value: CharArray): StringBuilder

    /**
     * Appends the specified string [value] to this string builder and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are appended.
     */
    @SinceKotlin("1.3")
    fun append(value: String?): StringBuilder

    /**
     * Returns the current capacity of this string builder.
     *
     * The capacity is the maximum length this string builder can have before an allocation occurs.
     *
     * In Kotlin/JS implementation of StringBuilder the value returned from this method may not indicate the actual size of the backing storage.
     */
    @SinceKotlin("1.3")
    fun capacity(): Int

    /**
     * Ensures that the capacity of this string builder is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun ensureCapacity(minimumCapacity: Int)

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun indexOf(string: String): Int

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun indexOf(string: String, startIndex: Int): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The last occurrence of empty string `""` is considered to be at the index equal to `this.length`.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun lastIndexOf(string: String): Int

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun lastIndexOf(string: String, startIndex: Int): Int

    /**
     * Inserts the string representation of the specified boolean [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: Boolean): StringBuilder

    /**
     * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Byte): StringBuilder

    /**
     * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Short): StringBuilder

    /**
     * Inserts the string representation of the specified int [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Int): StringBuilder

    /**
     * Inserts the string representation of the specified long [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Long): StringBuilder

    /**
     * Inserts the string representation of the specified float [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Float): StringBuilder

    /**
     * Inserts the string representation of the specified double [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.9")
    fun insert(index: Int, value: Double): StringBuilder

    /**
     * Inserts the specified character [value] into this string builder at the specified [index] and returns this instance.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: Char): StringBuilder

    /**
     * Inserts characters in the specified character array [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in same order as in the [value] character array, starting at [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: CharArray): StringBuilder

    /**
     * Inserts characters in the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the character sequence from which characters are inserted. If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: CharSequence?): StringBuilder

    /**
     * Inserts the string representation of the specified object [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: Any?): StringBuilder

    /**
     * Inserts the string [value] into this string builder at the specified [index] and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun insert(index: Int, value: String?): StringBuilder

    /**
     *  Sets the length of this string builder to the specified [newLength].
     *
     *  If the [newLength] is less than the current length, it is changed to the specified [newLength].
     *  Otherwise, null characters '\u0000' are appended to this string builder until its length is less than the [newLength].
     *
     *  Note that in Kotlin/JS [set] operator function has non-constant execution time complexity.
     *  Therefore, increasing length of this string builder and then updating each character by index may slow down your program.
     *
     *  @throws IndexOutOfBoundsException or [IllegalArgumentException] if [newLength] is less than zero.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun setLength(newLength: Int)

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [length] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun substring(startIndex: Int): String

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun substring(startIndex: Int, endIndex: Int): String

    /**
     * Attempts to reduce storage used for this string builder.
     *
     * If the backing storage of this string builder is larger than necessary to hold its current contents,
     * then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value of the [capacity] property.
     */
    @SinceKotlin("1.4")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun trimToSize()
}


/**
 * Clears the content of this string builder making it empty and returns this instance.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public expect fun StringBuilder.clear(): StringBuilder

/**
 * Sets the character at the specified [index] to the specified [value].
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect operator fun StringBuilder.set(index: Int, value: Char)

/**
 * Replaces characters in the specified range of this string builder with characters in the specified string [value] and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to replace.
 * @param endIndex the end (exclusive) of the range to replace.
 * @param value the string to replace with.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.deleteAt(index: Int): StringBuilder

/**
 * Removes characters in the specified range from this string builder and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to remove.
 * @param endIndex the end (exclusive) of the range to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.deleteRange(startIndex: Int, endIndex: Int): StringBuilder

/**
 * Copies characters from this string builder into the [destination] character array.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the array to copy to, 0 by default.
 * @param startIndex the beginning (inclusive) of the range to copy, 0 by default.
 * @param endIndex the end (exclusive) of the range to copy, length of this string builder by default.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
 *  or when that index is out of the [destination] array indices range.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length)

/**
 * Appends characters in a subarray of the specified character array [value] to this string builder and returns this instance.
 *
 * Characters are appended in order, starting at specified [startIndex].
 *
 * @param value the array from which characters are appended.
 * @param startIndex the beginning (inclusive) of the subarray to append.
 * @param endIndex the end (exclusive) of the subarray to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder

/**
 * Appends a subsequence of the specified character sequence [value] to this string builder and returns this instance.
 *
 * @param value the character sequence from which a subsequence is appended.
 * @param startIndex the beginning (inclusive) of the subsequence to append.
 * @param endIndex the end (exclusive) of the subsequence to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder

/**
 * Inserts characters in a subarray of the specified character array [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in same order as in the [value] array, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the array from which characters are inserted.
 * @param startIndex the beginning (inclusive) of the subarray to insert.
 * @param endIndex the end (exclusive) of the subarray to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder

/**
 * Inserts characters in a subsequence of the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the character sequence from which a subsequence is inserted.
 * @param startIndex the beginning (inclusive) of the subsequence to insert.
 * @param endIndex the end (exclusive) of the subsequence to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun StringBuilder.insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use append(value: Any?) instead", ReplaceWith("append(value = obj)"), DeprecationLevel.WARNING)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.append(obj: Any?): StringBuilder = this.append(obj)

/**
 * Builds new string by populating newly created [StringBuilder] using provided [builderAction]
 * and then converting it to [String].
 */
@kotlin.internal.InlineOnly
public inline fun buildString(builderAction: StringBuilder.() -> Unit): String {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return StringBuilder().apply(builderAction).toString()
}

/**
 * Builds new string by populating newly created [StringBuilder] initialized with the given [capacity]
 * using provided [builderAction] and then converting it to [String].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun buildString(capacity: Int, builderAction: StringBuilder.() -> Unit): String {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return StringBuilder(capacity).apply(builderAction).toString()
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: String?): StringBuilder {
    for (item in value)
        append(item)
    return this
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: Any?): StringBuilder {
    for (item in value)
        append(item)
    return this
}

// KT-52336
@Deprecated("Use appendRange instead.", ReplaceWith("this.appendRange(str, offset, offset + len)"), level = DeprecationLevel.ERROR)
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public inline fun StringBuilder.append(str: CharArray, offset: Int, len: Int): StringBuilder = throw NotImplementedError()

/** Appends a line feed character (`\n`) to this StringBuilder. */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(): StringBuilder = append('\n')

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: CharSequence?): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: String?): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Any?): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: CharArray): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Char): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Boolean): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Byte): StringBuilder

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Short): StringBuilder

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Int): StringBuilder

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Long): StringBuilder

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Float): StringBuilder

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.9")
public expect fun StringBuilder.appendLine(value: Double): StringBuilder