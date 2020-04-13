/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Short): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()


@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: String): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Boolean): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Byte): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Short): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Int): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Long): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Float): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Double): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine(it)"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(it: Any?): StringBuilder = appendLine(it)

@Deprecated("Use appendLine instead", ReplaceWith("appendLine()"), level = DeprecationLevel.WARNING)
public fun StringBuilder.appendln(): StringBuilder = appendLine()