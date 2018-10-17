/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

/**
 * Sets the character at the specified [index] to the specified [value].
 */
@kotlin.internal.InlineOnly
public inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.setCharAt(index, value)

/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public actual fun StringBuilder.clear(): StringBuilder = apply { setLength(0) }

private object SystemProperties {
    /** Line separator for current system. */
    @JvmField
    val LINE_SEPARATOR = System.getProperty("line.separator")!!
}

/** Appends a line separator to this Appendable. */
public fun Appendable.appendln(): Appendable = append(SystemProperties.LINE_SEPARATOR)

/** Appends value to the given Appendable and line separator after it. */
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: CharSequence?): Appendable = append(value).appendln()

/** Appends value to the given Appendable and line separator after it. */
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: Char): Appendable = append(value).appendln()

/** Appends a line separator to this StringBuilder. */
public fun StringBuilder.appendln(): StringBuilder = append(SystemProperties.LINE_SEPARATOR)

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuffer?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharSequence?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: String?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Any?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuilder?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharArray): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Char): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Boolean): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Int): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Short): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Byte): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Long): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Float): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Double): StringBuilder = append(value).appendln()
