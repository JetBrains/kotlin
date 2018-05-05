/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.js.RegExp

@kotlin.internal.InlineOnly
public actual inline fun String.toUpperCase(): String = asDynamic().toUpperCase()

@kotlin.internal.InlineOnly
public actual inline fun String.toLowerCase(): String = asDynamic().toLowerCase()

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeIndexOf(str: String, fromIndex: Int): Int = asDynamic().indexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = asDynamic().lastIndexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal inline fun String.nativeStartsWith(s: String, position: Int): Boolean = asDynamic().startsWith(s, position)

@kotlin.internal.InlineOnly
internal inline fun String.nativeEndsWith(s: String): Boolean = asDynamic().endsWith(s)

@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int): String = asDynamic().substring(startIndex)

@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int, endIndex: Int): String = asDynamic().substring(startIndex, endIndex)

@kotlin.internal.InlineOnly
public inline fun String.concat(str: String): String = asDynamic().concat(str)

@kotlin.internal.InlineOnly
public inline fun String.match(regex: String): Array<String>? = asDynamic().match(regex)

//native public fun String.trim(): String
//TODO: String.replace to implement effective trimLeading and trimTrailing

@Deprecated("Use length property instead.", ReplaceWith("length"), level = DeprecationLevel.ERROR)
@kotlin.internal.InlineOnly
public inline val CharSequence.size: Int get() = length

@kotlin.internal.InlineOnly
internal inline fun String.nativeReplace(pattern: RegExp, replacement: String): String = asDynamic().replace(pattern, replacement)
