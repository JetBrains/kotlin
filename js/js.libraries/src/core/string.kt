package kotlin.text

import kotlin.text.js.RegExp

public external fun String.toUpperCase(): String = noImpl

public external fun String.toLowerCase(): String = noImpl

@JsName("indexOf")
internal external fun String.nativeIndexOf(str: String, fromIndex: Int): Int = noImpl

@JsName("lastIndexOf")
internal external fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = noImpl

@JsName("startsWith")
internal external fun String.nativeStartsWith(s: String, position: Int): Boolean = noImpl

@JsName("endsWith")
internal external fun String.nativeEndsWith(s: String): Boolean = noImpl

@Deprecated("Use split(Regex) instead.", ReplaceWith("split(regex.toRegex()).toTypedArray()"))
@library("splitString")
public fun String.splitWithRegex(regex: String): Array<String> = noImpl

@Deprecated("Use split(Regex) instead.", ReplaceWith("split(regex.toRegex(), limit = limit).toTypedArray()"))
@library("splitString")
public fun String.splitWithRegex(regex: String, limit: Int): Array<String> = noImpl

public external fun String.substring(startIndex: Int): String = noImpl

public external fun String.substring(startIndex: Int, endIndex: Int): String = noImpl

public external fun String.concat(str: String): String = noImpl

public external fun String.match(regex: String): Array<String> = noImpl

//native public fun String.trim() : String = noImpl
//TODO: String.replace to implement effective trimLeading and trimTrailing

public inline val CharSequence.size: Int get() = asDynamic().length

@JsName("replace")
internal external fun String.nativeReplace(pattern: RegExp, replacement: String): String = noImpl
