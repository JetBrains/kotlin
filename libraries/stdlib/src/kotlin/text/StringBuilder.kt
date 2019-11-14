/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

expect class StringBuilder : Appendable, CharSequence {
    constructor()
    constructor(capacity: Int)
    constructor(content: CharSequence)

    override val length: Int
    override operator fun get(index: Int): Char
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    fun reverse(): StringBuilder
    override fun append(c: Char): StringBuilder
    override fun append(csq: CharSequence?): StringBuilder
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder
    fun append(obj: Any?): StringBuilder
}

/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public expect fun StringBuilder.clear(): StringBuilder

/**
 * Builds new string by populating newly created [StringBuilder] using provided [builderAction]
 * and then converting it to [String].
 */
@kotlin.internal.InlineOnly
public inline fun buildString(builderAction: StringBuilder.() -> Unit): String =
    StringBuilder().apply(builderAction).toString()

/**
 * Builds new string by populating newly created [StringBuilder] initialized with the given [capacity]
 * using provided [builderAction] and then converting it to [String].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun buildString(capacity: Int, builderAction: StringBuilder.() -> Unit): String =
    StringBuilder(capacity).apply(builderAction).toString()

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
