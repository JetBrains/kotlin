/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behavior of [PathInclusionRule]s as specified in 'repo/TEST-FEDERATION.md' using made-up rules and paths.
 */
class PathInclusionRulesTest {

    private fun rules(vararg rules: String): PathInclusionRule = compilePathInclusionRules(rules.toList())

    private fun PathInclusionRule.includes(path: String): Boolean? = includes(RepositoryPath(Path.of("."), Path.of(path)))

    @Test
    fun `matching path is included`() {
        val rules = rules("foo", "bar/*.txt")
        assertEquals(true, rules.includes("foo"))
        assertEquals(true, rules.includes("bar/a.txt"))
        assertEquals(null, rules.includes("baz"))
        assertEquals(null, rules.includes("bar/a.kt"))
    }

    @Test
    fun `negated rule excludes matching path`() {
        val rules = rules("!foo")
        assertEquals(false, rules.includes("foo"))
        assertEquals(null, rules.includes("bar"))
    }

    @Test
    fun `last matching rule wins`() {
        val rules = rules("foo/*", "!foo/bar")
        assertEquals(false, rules.includes("foo/bar"))
        assertEquals(true, rules.includes("foo/baz"))
    }

    @Test
    fun `order of rules matters`() {
        val rules = rules("!foo/bar", "foo/*")
        assertEquals(true, rules.includes("foo/bar"))
    }

    @Test
    fun `negated rule can be re-negated by a later rule`() {
        val rules = rules("**/*.md", "!**/README.md", "foo/README.md")
        assertEquals(true, rules.includes("foo/a.md"))
        assertEquals(false, rules.includes("bar/README.md"))
        assertEquals(true, rules.includes("foo/README.md"))
    }

    @Test
    fun `plus - other rule wins if it matches`() {
        val combined = rules("foo/*") + !rules("foo/bar")
        assertEquals(false, combined.includes("foo/bar"))
        assertEquals(true, combined.includes("foo/baz"))
        assertEquals(null, combined.includes("baz"))
    }

    @Test
    fun `not - inverts included and excluded paths`() {
        val inverted = !rules("foo", "!bar")
        assertEquals(false, inverted.includes("foo"))
        assertEquals(true, inverted.includes("bar"))
        assertEquals(null, inverted.includes("baz"))
    }

    @Test
    fun `withExcluded - excluded rule narrows included paths`() {
        val combined = rules("foo/*") withExcluded rules("foo/bar")
        assertEquals(false, combined.includes("foo/bar"))
        assertEquals(true, combined.includes("foo/baz"))
        assertEquals(null, combined.includes("baz"))
    }

    @Test
    fun `withExcluded - excluded rule cannot include a path`() {
        val combined = rules("foo/*") withExcluded rules("bar", "!baz")
        assertEquals(false, combined.includes("bar"))
        assertEquals(null, combined.includes("baz"))
    }

    @Test
    fun `withExcluded - negated excluded rule revokes a previous exclusion`() {
        val combined = rules("foo/*") withExcluded rules("foo/*", "!foo/bar")
        assertEquals(false, combined.includes("foo/baz"))
        assertEquals(true, combined.includes("foo/bar"))
    }

    @Test
    fun `withExcluded - excluded rule wins even if included rule does not match`() {
        /* Required so that an excluded directory wins over an included parent directory */
        val combined = rules("foo") withExcluded rules("foo/bar")
        assertEquals(false, combined.includes("foo/bar"))
        assertEquals(true, combined.includes("foo"))
    }
}
