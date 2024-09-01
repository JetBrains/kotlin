/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

import kotlin.native.internal.GCUnsafeCall

// Access to the decomposition tables. =========================================================================
/** Gets canonical class for given codepoint from decomposition mappings table. */
@GCUnsafeCall("Kotlin_text_regex_getCanonicalClassInternal")
internal actual external fun getCanonicalClassInternal(ch: Int): Int

/** Check if the given character is in table of single decompositions. */
@GCUnsafeCall("Kotlin_text_regex_hasSingleCodepointDecompositionInternal")
internal actual external fun hasSingleCodepointDecompositionInternal(ch: Int): Boolean

/** Returns a decomposition for a given codepoint. */
@GCUnsafeCall("Kotlin_text_regex_getDecompositionInternal")
internal external fun getDecompositionInternal(ch: Int): IntArray?

/**
 * Decomposes the given string represented as an array of codepoints. Saves the decomposition into [outputCodepoints] array.
 * Returns the length of the decomposition.
 */
@GCUnsafeCall("Kotlin_text_regex_decomposeString")
internal actual external fun decomposeString(inputCodePoints: IntArray, inputLength: Int, outputCodePoints: IntArray): Int

/**
 * Decomposes the given codepoint. Saves the decomposition into [outputCodepoints] array starting with [fromIndex].
 * Returns the length of the decomposition.
 */
@GCUnsafeCall("Kotlin_text_regex_decomposeCodePoint")
internal actual external fun decomposeCodePoint(codePoint: Int, outputCodePoints: IntArray, fromIndex: Int): Int
// =============================================================================================================