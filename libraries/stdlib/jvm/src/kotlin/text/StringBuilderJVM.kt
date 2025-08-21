/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

/**
 * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.append(value: Byte): StringBuilder = this.append(value.toInt())

/**
 * Appends the string representation of the specified short [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.append(value: Short): StringBuilder = this.append(value.toInt())

/**
 * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insert(index: Int, value: Byte): StringBuilder = this.insert(index, value.toInt())

/**
 * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insert(index: Int, value: Short): StringBuilder = this.insert(index, value.toInt())

/**
 * Clears the content of this string builder making it empty and returns this instance.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
@IgnorableReturnValue
public actual fun StringBuilder.clear(): StringBuilder = apply { setLength(0) }

/**
 * Sets the character at the specified [index] to the specified [value].
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@kotlin.internal.InlineOnly
public actual inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder =
    this.replace(startIndex, endIndex, value)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.deleteAt(index: Int): StringBuilder = this.deleteCharAt(index)

/**
 * Removes characters in the specified range from this string builder and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to remove.
 * @param endIndex the end (exclusive) of the range to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.deleteRange(startIndex: Int, endIndex: Int): StringBuilder = this.delete(startIndex, endIndex)

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
@kotlin.internal.InlineOnly
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length): Unit =
    this.getChars(startIndex, endIndex, destination, destinationOffset)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.append(value, startIndex, endIndex - startIndex)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
    this.append(value, startIndex, endIndex)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
    this.insert(index, value, startIndex, endIndex - startIndex)

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
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
    this.insert(index, value, startIndex, endIndex)


/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public inline fun StringBuilder.appendLine(value: StringBuffer?): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public inline fun StringBuilder.appendLine(value: StringBuilder?): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Short): StringBuilder = append(value.toInt()).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value.toInt()).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()


private object SystemProperties {
    /** Line separator for current system. */
    @JvmField
    val LINE_SEPARATOR = System.getProperty("line.separator")!!
}

/** Appends a line separator to this Appendable. */
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine()")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
public fun Appendable.appendln(): Appendable = append(SystemProperties.LINE_SEPARATOR)

/** Appends value to the given Appendable and line separator after it. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: CharSequence?): Appendable = append(value).appendln()

/** Appends value to the given Appendable and line separator after it. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: Char): Appendable = append(value).appendln()

/** Appends a line separator to this StringBuilder. */
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine()")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
public fun StringBuilder.appendln(): StringBuilder = append(SystemProperties.LINE_SEPARATOR)

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuffer?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharSequence?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: String?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Any?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuilder?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharArray): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Char): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Boolean): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Int): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Short): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Byte): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Long): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Float): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use appendLine instead. Note that the new method always appends the line feed character '\\n' regardless of the system line separator.",
    ReplaceWith("appendLine(value)")
)
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "2.1")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Double): StringBuilder = append(value).appendln()
