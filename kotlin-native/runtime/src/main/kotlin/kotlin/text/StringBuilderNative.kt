/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.native.internal.GCUnsafeCall

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Short): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()


@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: String): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Boolean): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Byte): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Short): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Int): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Long): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Float): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Double): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"))
public fun StringBuilder.appendln(it: Any?): StringBuilder = appendLine(it)

@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use appendLine instead", ReplaceWith("appendLine()"))
public fun StringBuilder.appendln(): StringBuilder = appendLine()

@GCUnsafeCall("Kotlin_StringBuilder_insertString")
internal external fun insertString(array: CharArray, distIndex: Int, value: String, sourceIndex: Int, count: Int): Int

@GCUnsafeCall("Kotlin_StringBuilder_insertInt")
internal external fun insertInt(array: CharArray, start: Int, value: Int): Int