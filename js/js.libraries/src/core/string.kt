package kotlin.text

import kotlin.text.js.RegExp

public inline fun String.toUpperCase(): String = asDynamic().toUpperCase()

public inline fun String.toLowerCase(): String = asDynamic().toLowerCase()

internal inline fun String.nativeIndexOf(str: String, fromIndex: Int): Int = asDynamic().indexOf(str, fromIndex)

internal inline fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = asDynamic().lastIndexOf(str, fromIndex)

internal inline fun String.nativeStartsWith(s: String, position: Int): Boolean = asDynamic().startsWith(s, position)

internal inline fun String.nativeEndsWith(s: String): Boolean = asDynamic().endsWith(s)

public inline fun String.substring(startIndex: Int): String = asDynamic().substring(startIndex)

public inline fun String.substring(startIndex: Int, endIndex: Int): String = asDynamic().substring(startIndex, endIndex)

public inline fun String.concat(str: String): String = asDynamic().concat(str)

public inline fun String.match(regex: String): Array<String> = asDynamic().match(regex)

//native public fun String.trim() : String = noImpl
//TODO: String.replace to implement effective trimLeading and trimTrailing

public inline val CharSequence.size: Int get() = asDynamic().length

internal inline fun String.nativeReplace(pattern: RegExp, replacement: String): String = asDynamic().replace(pattern, replacement)
