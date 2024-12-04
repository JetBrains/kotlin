/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the JavaScript [RegExp object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/RegExp) to Kotlin.
 */
@Suppress("NOT_DOCUMENTED")
public external class RegExp(pattern: String, flags: String? = definedExternally) {

    public fun test(str: String): Boolean

    public fun exec(str: String): RegExpMatch?

    public override fun toString(): String

    /**
     * The lastIndex is a read/write integer property of regular expressions that specifies the index at which to start the next match.
     */
    public var lastIndex: Int

    public val global: Boolean
    public val ignoreCase: Boolean
    public val multiline: Boolean
}

/**
 * Resets the regular expression so that subsequent [RegExp.test] and [RegExp.exec] calls will match starting with the beginning of the input string.
 */
public fun RegExp.reset() {
    lastIndex = 0
}

// TODO: Inherit from array or introduce asArray() extension
/**
 * Represents the return value of [RegExp.exec].
 */
@Suppress("NOT_DOCUMENTED")
public external interface RegExpMatch {
    public val index: Int
    public val input: String
    public val length: Int
}

/**
 * Returns the entire text matched by [RegExp.exec] if the [index] parameter is 0, or the text matched by the capturing parenthesis
 * at the given index.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun RegExpMatch.get(index: Int): String? = asDynamic()[index]

/**
 * Converts the result of [RegExp.exec] to an array where the first element contains the entire matched text and each subsequent
 * element is the text matched by each capturing parenthesis.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun RegExpMatch.asArray(): Array<out String?> = unsafeCast<Array<out String?>>()
