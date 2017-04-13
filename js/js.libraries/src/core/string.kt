package kotlin.text

import kotlin.js.RegExp

@kotlin.internal.InlineOnly
public inline fun String.toUpperCase(): String = asDynamic().toUpperCase()

@kotlin.internal.InlineOnly
public inline fun String.toLowerCase(): String = asDynamic().toLowerCase()

@kotlin.internal.InlineOnly
internal inline fun String.nativeIndexOf(str: String, fromIndex: Int): Int = asDynamic().indexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal inline fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = asDynamic().lastIndexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal inline fun String.nativeStartsWith(s: String, position: Int): Boolean = asDynamic().startsWith(s, position)

@kotlin.internal.InlineOnly
internal inline fun String.nativeEndsWith(s: String): Boolean = asDynamic().endsWith(s)

@kotlin.internal.InlineOnly
public inline fun String.substring(startIndex: Int): String = asDynamic().substring(startIndex)

@kotlin.internal.InlineOnly
public inline fun String.substring(startIndex: Int, endIndex: Int): String = asDynamic().substring(startIndex, endIndex)

@kotlin.internal.InlineOnly
public inline fun String.concat(str: String): String = asDynamic().concat(str)

@kotlin.internal.InlineOnly
public inline fun String.match(regex: String): Array<String>? = asDynamic().match(regex)

//native public fun String.trim(): String
//TODO: String.replace to implement effective trimLeading and trimTrailing

@kotlin.internal.InlineOnly
public inline val CharSequence.size: Int get() = asDynamic().length

@kotlin.internal.InlineOnly
internal inline fun String.nativeReplace(pattern: RegExp, replacement: String): String = asDynamic().replace(pattern, replacement)
