package kotlin.text

import kotlin.text.js.RegExp

@native public fun String.toUpperCase(): String = noImpl

@native public fun String.toLowerCase(): String = noImpl

@native("indexOf")
internal fun String.nativeIndexOf(str: String, fromIndex: Int): Int = noImpl

@native("lastIndexOf")
internal fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = noImpl

@native("startsWith")
internal fun String.nativeStartsWith(s: String, position: Int): Boolean = noImpl

@native("endsWith")
internal fun String.nativeEndsWith(s: String): Boolean = noImpl

@Deprecated("Use split(Regex) instead.", ReplaceWith("split(regex.toRegex()).toTypedArray()"))
@library("splitString")
public fun String.splitWithRegex(regex: String): Array<String> = noImpl

@Deprecated("Use split(Regex) instead.", ReplaceWith("split(regex.toRegex(), limit = limit).toTypedArray()"))
@library("splitString")
public fun String.splitWithRegex(regex: String, limit: Int): Array<String> = noImpl

@native public fun String.substring(startIndex: Int): String = noImpl

@native public fun String.substring(startIndex: Int, endIndex: Int): String = noImpl

@native public fun String.concat(str: String): String = noImpl

@native public fun String.match(regex: String): Array<String> = noImpl

//native public fun String.trim() : String = noImpl
//TODO: String.replace to implement effective trimLeading and trimTrailing

@native("length")
public val CharSequence.size: Int get() = noImpl

@native("replace")
internal fun String.nativeReplace(pattern: RegExp, replacement: String): String = noImpl
