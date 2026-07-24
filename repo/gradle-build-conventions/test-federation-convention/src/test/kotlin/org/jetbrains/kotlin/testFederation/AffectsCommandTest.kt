/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import kotlin.test.Test
import kotlin.test.assertEquals

class AffectsCommandTest {

    @Test
    fun `test - no command`() {
        assertEquals(emptySet(), resolveAffectedDomainsFromCommitMessages(listOf("no affects command")))
    }

    @Test
    fun `test - single domain`() {
        assertEquals(setOf(Domain.Gradle), resolveAffectedDomainsFromCommitMessages(listOf("^affects: Gradle")))
    }

    @Test
    fun `test - multiple domains - different separators`() {
        assertEquals(
            setOf(Domain.Gradle, Domain.IntelliJ, Domain.AnalysisApi, Domain.Compiler),
            resolveAffectedDomainsFromCommitMessages(listOf("^affects: Gradle, IntelliJ AnalysisApi; Compiler"))
        )
    }

    @Test
    fun `test - multiple commands - in multiple messages`() {
        assertEquals(
            setOf(Domain.Gradle, Domain.IntelliJ, Domain.AnalysisApi, Domain.Compiler),
            resolveAffectedDomainsFromCommitMessages(
                listOf(
                    """
                    ^affects: Gradle
                    ^affects: IntelliJ
                    """.trimIndent(),
                    """
                    ^affects: AnalysisApi
                    foo
                    bar
                    ^affects: Compiler
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `test - star notation`() {
        assertEquals(Domain.entries.toSet(), resolveAffectedDomainsFromCommitMessages(listOf("^affects: *")))
    }
}
