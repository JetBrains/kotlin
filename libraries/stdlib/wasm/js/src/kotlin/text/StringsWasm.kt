/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int =
    nativeIndexOf(ch.toString(), fromIndex)

@OptIn(ExperimentalWasmJsInterop::class)
private fun nativeIndexOf(source: String, substring: String, fromIndex: Int): Int =
    js("source.indexOf(substring, fromIndex)")

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int =
    nativeIndexOf(this, str, fromIndex)

@OptIn(ExperimentalWasmJsInterop::class)
private fun nativeRepeat(str: String, n: Int): String =
    js("str.repeat(n)")

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public actual fun CharSequence.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }

    return nativeRepeat(this.toString(), n)
}

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
    if (!ignoreCase)
        nativeReplace(this.internalStr, oldChar.toString().internalStr, newChar.toString().internalStr)
    else
        nativeReplaceIgnore(this.internalStr, oldChar.toString().internalStr, newChar.toString().internalStr)
/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
    if (!ignoreCase)
        nativeReplace(this.internalStr, oldValue.internalStr, newValue.internalStr)
    else
        nativeReplaceIgnore(this.internalStr, oldValue.internalStr, newValue.internalStr)

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
    if (!ignoreCase)
        nativeReplaceFirst(this.internalStr, oldChar.toString().internalStr, newChar.toString().internalStr)
    else
        nativeReplaceFirstIgnore(this.internalStr, oldChar.toString().internalStr, newChar.toString().internalStr)

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
    if (!ignoreCase)
        nativeReplaceFirst(this.internalStr, oldValue.internalStr, newValue.internalStr)
    else
        nativeReplaceFirstIgnore(this.internalStr, oldValue.internalStr, newValue.internalStr)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(() => {
    const patternEscapeToReplace = /[\\^$*+?.()|[\]{}]/g;
    const patternEscapeReplacement = /\$/g;

    return function nativeReplaceFirst(source, oldValue, newValue) {
        const replacement = newValue.replace(patternEscapeReplacement, '$$$$');
        const escaped = oldValue.replace(patternEscapeToReplace, '\\$&');
        return source.replace(new RegExp(escaped, 'gu'), replacement);
    };
})()"""
)
private external fun nativeReplace(source: JsString, oldValue: JsString, newValue: JsString): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(() => {
    const patternEscapeToReplace = /[\\^$*+?.()|[\]{}]/g;
    const patternEscapeReplacement = /\$/g;

    return function nativeReplaceFirst(source, oldValue, newValue) {
        const replacement = newValue.replace(patternEscapeReplacement, '$$$$');
        const escaped = oldValue.replace(patternEscapeToReplace, '\\$&');
        return source.replace(new RegExp(escaped, 'gui'), replacement);
    };
})()"""
)
private external fun nativeReplaceIgnore(source: JsString, oldValue: JsString, newValue: JsString): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(() => {
    const patternEscapeToReplace = /[\\^$*+?.()|[\]{}]/g;
    const patternEscapeReplacement = /\$/g;

    return function nativeReplaceFirst(source, oldValue, newValue) {
        const replacement = newValue.replace(patternEscapeReplacement, '$$$$');
        const escaped = oldValue.replace(patternEscapeToReplace, '\\$&');
        return source.replace(new RegExp(escaped, 'u'), replacement);
    };
})()"""
)
private external fun nativeReplaceFirst(source: JsString, oldValue: JsString, newValue: JsString): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(() => {
    const patternEscapeToReplace = /[\\^$*+?.()|[\]{}]/g;
    const patternEscapeReplacement = /\$/g;

    return function nativeReplaceFirst(source, oldValue, newValue) {
        const replacement = newValue.replace(patternEscapeReplacement, '$$$$');
        const escaped = oldValue.replace(patternEscapeToReplace, '\\$&');
        return source.replace(new RegExp(escaped, 'ui'), replacement);
    };
})()"""
)
private external fun nativeReplaceFirstIgnore(source: JsString, oldValue: JsString, newValue: JsString): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(str) => [...str].reverse().join('')")
internal external fun reverseJsString(source: JsString): JsString