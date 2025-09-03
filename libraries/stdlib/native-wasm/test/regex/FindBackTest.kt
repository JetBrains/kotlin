/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.regex

import kotlin.test.Test
import kotlin.test.assertEquals

// Regression tests for KT-61180.
// Tests cover the following scenario:
// - a regex has some prefix, followed by a ".*", followed by some AbstractSet
//   (like CharSet or RangeSet, i.e. "c" or "[0-9]"), followed by something else;
// - a findAll is invoked, and there is a match;
// - after that match, the input contains substring matching Regex's prefix (before ".*"), however,
//   the only occurrence of a sub-regex after ".*" is before the beginning of a substring being currently matched.
//
// Here's an example:
// Regex: "a.*c\\d", where
// - "a" is a prefix
// - "c\\d" is a suffix
// Input: "abc0aaaa", such as:
// - "abc0" is a first full match;
// - it is followed by the "a", which matches the prefix;
// - .* consumes everything until the end of the input;
// - "c\\d" has to be back-searched until the "abc0", but the search should not overlap with it.
class FindBackTest {
    @Test
    fun findBackCharSetAfterDotKleene() {
        val re = Regex("a.*c\\d")
        val text = "abc0aaaa"

        assertEquals(listOf("abc0"), re.findAll(text).map { it.value }.toList())
    }

    @Test
    fun findBackClassAfterDotKleene() {
        val re = Regex("a.*\\p{Alpha}\\d")
        val text = "abc0aaaa"

        assertEquals(listOf("abc0"), re.findAll(text).map { it.value }.toList())
    }

    @Test
    fun findBackSequenceAfterDotKleene() {
        val re = Regex("a.*bc\\d")
        val text = "abbc0aaaa"

        assertEquals(listOf("abbc0"), re.findAll(text).map { it.value }.toList())
    }

    @Test
    fun findBackHighSurrogateAfterDotKleene() {
        val re = Regex("a.*\uD83C\\d")
        val text = "ab\uD83C0aaaa"

        assertEquals(listOf("ab\uD83C0"), re.findAll(text).map { it.value }.toList())
    }

    @Test
    fun findBackLowSurrogateAfterDotKleene() {
        val re = Regex("a.*\uDF1A\\d")
        val text = "ab\uDF1A0aaaa"

        assertEquals(listOf("ab\uDF1A0"), re.findAll(text).map { it.value }.toList())
    }

    @Test
    fun findBackSurrogateRangeAfterDotKleene() {
        val re = Regex("a.*\\p{Cs}\\d")
        val text = "ab\uDF1A0aaaa"

        assertEquals(listOf("ab\uDF1A0"), re.findAll(text).map { it.value }.toList())
    }

    // we don't have a lot of custom findBack implementations, this test covers default implementation
    @Test
    fun findBackOtherAfterDotKleene() {
        val re = Regex("a.*c{2}\\d")
        val text = "abcc0aaaaa"

        assertEquals(listOf("abcc0"), re.findAll(text).map { it.value }.toList())
    }
}
