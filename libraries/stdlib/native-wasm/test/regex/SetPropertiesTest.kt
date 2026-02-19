/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.regex.sets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.regex.SetProperties
import kotlin.text.regex.SingleSet

class SetPropertiesTest {
    private fun assertProperties(
        pattern: String,
        capturesGroups: Boolean,
        tracksConsumption: Boolean,
        nonTrivialBacktracking: Boolean,
        requireCheckpointing: Boolean
    ) {
        val re = Regex(pattern)
        val p = re.nativePattern
        val properties = SetProperties()
        p.startNode.collectProperties(properties)
        assertEquals(
            SetProperties(
                capturesGroups = capturesGroups, tracksConsumption = tracksConsumption,
                nonTrivialBacktracking = nonTrivialBacktracking, requiresCheckpointing = requireCheckpointing
            ),
            properties,
            "For a regular expression \"$pattern\""
        )
    }

    private fun assertInnerSetProperties(
        pattern: String,
        capturesGroups: Boolean,
        tracksConsumption: Boolean,
        nonTrivialBacktracking: Boolean,
        requireCheckpointing: Boolean
    ) {
        val re = Regex(pattern)
        val p = re.nativePattern
        val properties = SetProperties()
        val set = p.startNode as SingleSet
        set.kid.collectProperties(properties, set.fSet)
        assertEquals(
            SetProperties(
                capturesGroups = capturesGroups, tracksConsumption = tracksConsumption,
                nonTrivialBacktracking = nonTrivialBacktracking, requiresCheckpointing = requireCheckpointing
            ),
            properties,
            "For a regular expression \"$pattern\""
        )
    }

    @Test
    fun testProperties() {
        assertProperties("abc", capturesGroups = true, false, false, false)
        assertProperties("(abc)", capturesGroups = true, false, false, false)
        assertProperties("(abc*)", capturesGroups = true, false, nonTrivialBacktracking = true, false)
        assertProperties("(?:a|aa)", capturesGroups = true, tracksConsumption = true, nonTrivialBacktracking = true, false)
        assertProperties("(?<=a)b", capturesGroups = true, true, nonTrivialBacktracking = true, requireCheckpointing = true)
    }

    // Every regex has a top-level group corresponding to a regular expression as a whole, so capturesGroups is always true
    // This test inspects its inner set for exactly the same REs as testProperties, to ensure capturesGroups is captured correctly
    @Test
    fun testInnerSetProperties() {
        assertInnerSetProperties("abc", capturesGroups = false, false, false, false)
        assertInnerSetProperties("(abc)", capturesGroups = true, false, false, false)
        assertInnerSetProperties("(abc*)", capturesGroups = true, false, nonTrivialBacktracking = true, false)
        assertInnerSetProperties("(?:a|aa)", capturesGroups = false, tracksConsumption = true, nonTrivialBacktracking = true, false)
        assertInnerSetProperties("(?<=a)b", capturesGroups = false, tracksConsumption = true, nonTrivialBacktracking = true, requireCheckpointing = true)
    }

    @Test
    fun testNonTrivialRegularExpression() {
        assertInnerSetProperties("a(?:b*)*c", capturesGroups = false, tracksConsumption = true, nonTrivialBacktracking = true, requireCheckpointing = false)
    }

    @Test
    fun testTrivialRegularExpressions() {
        // EOL tracks consumption, others - don't
        assertInnerSetProperties("abc$", false, true, false, false)
        assertInnerSetProperties("^abc", false, false, false, false)
        assertInnerSetProperties("abc\\z", false, false, false, false)
        assertInnerSetProperties("\\B", false, false, false, false)
        assertInnerSetProperties("\\b", false, false, false, false)
        assertInnerSetProperties("\\G", false, false, false, false)
        assertInnerSetProperties("[a-z]", false, false, false, false)
        assertInnerSetProperties("\\P{Nd}", false, false, false, false)
        assertInnerSetProperties("\\p{Nd}", false, false, false, false)
        assertInnerSetProperties(".", false, false, false, false)
        assertInnerSetProperties("", false, false, false, false)
    }

    @Test
    fun testQuantifiers() {
        fun checkEagerLazyAndPossessive(
            pattern: String,
            capturesGroups: Boolean,
            tracksConsumption: Boolean,
            nonTrivialBacktracking: Boolean,
            requireCheckpointing: Boolean
        ) {
            for (q in listOf("", "?", "+")) {
                assertInnerSetProperties(pattern + q, capturesGroups, tracksConsumption, nonTrivialBacktracking, requireCheckpointing)
            }
        }
        checkEagerLazyAndPossessive("a*", false, false, true, false)
        checkEagerLazyAndPossessive("a+", false, false, true, false)
        checkEagerLazyAndPossessive("a?", false, false, true, false)
        checkEagerLazyAndPossessive("a{1,}", false, false, true, false)
        checkEagerLazyAndPossessive("a{1,3}", false, false, true, false)
        // that's the only one that does not require complex backtracking
        checkEagerLazyAndPossessive("a{3}", false, false, false, false)

        assertInnerSetProperties(".*", false, false, true, false)
    }

    @Test
    fun testGroups() {
        assertInnerSetProperties("(a)", true, false, false, false)
        assertInnerSetProperties("((a))", true, false, false, false)
        assertInnerSetProperties("(?:a)", false, true, false, false)
        // look behinds
        assertInnerSetProperties("(?<=a)a", false, true, true, true)
        assertInnerSetProperties("(?<!a)a", false, true, true, true)
        // look ahead
        assertInnerSetProperties("(?=a)a", false, false, true, true)
        assertInnerSetProperties("(?!a)a", false, false, true, true)

        // joint sets
        assertInnerSetProperties("(a|aa)", true, false, true, false)
        assertInnerSetProperties("(?:a|aa)", false, true, true, false)
    }

    @Test
    fun testBackreference() {
        assertInnerSetProperties("([ab])\\1\\1", true, true, true, false)
    }
}
