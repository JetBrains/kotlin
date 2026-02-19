/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.*

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Byte): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Short): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Int): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Long): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Float): StringBuilder = append(value).appendLine()

/** Appends [value] to this [StringBuilder], followed by a line feed character (`\n`). */
@IgnorableReturnValue
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.appendLine(value: Double): StringBuilder = append(value).appendLine()
