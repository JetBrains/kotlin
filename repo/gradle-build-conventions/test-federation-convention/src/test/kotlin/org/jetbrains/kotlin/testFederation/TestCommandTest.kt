/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestCommandTest {

    @Test
    fun `test - no command`() {
        assertEquals(emptySet(), resolveDomainsFromCommitMessages(listOf("no test command")))
    }

    @Test
    fun `test - single domain`() = listOf("test", "affects").forEach { command ->
        assertEquals(setOf(Domain.Gradle), resolveDomainsFromCommitMessages(listOf("^$command: Gradle")))
    }

    @Test
    fun `test - multiple domains - different separators`() = listOf("test", "affects").forEach { command ->
        assertEquals(
            setOf(Domain.Gradle, Domain.IntelliJ, Domain.AnalysisApi, Domain.CompilerInfrastructure),
            resolveDomainsFromCommitMessages(listOf("^$command: Gradle, IntelliJ AnalysisApi; CompilerInfrastructure"))
        )
    }

    @Test
    fun `test - multiple commands - in multiple messages`() = listOf("test", "affects").forEach { command ->
        assertEquals(
            setOf(Domain.Gradle, Domain.IntelliJ, Domain.AnalysisApi, Domain.CompilerInfrastructure),
            resolveDomainsFromCommitMessages(
                listOf(
                    """
                    ^$command: Gradle
                    ^$command: IntelliJ
                    """.trimIndent(),
                    """
                    ^$command: AnalysisApi
                    foo
                    bar
                    ^$command: CompilerInfrastructure
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun `test - star notation`() = listOf("test", "affects").forEach { command ->
        assertEquals(Domain.entries.toSet(), resolveDomainsFromCommitMessages(listOf("^$command: *")))
    }

    @Test
    fun `test - mixed commands`() {
        assertEquals(
            setOf(Domain.Gradle, Domain.AnalysisApi),
            resolveDomainsFromCommitMessages(listOf("^test: Gradle\n^affects: AnalysisApi")),
        )
    }

    @Test
    fun `test - invalid domain`() = listOf("test", "affects").forEach { command ->
        assertFailsWith<IllegalArgumentException> {
            resolveDomainsFromCommitMessages(listOf("^$command: NotADomain"))
        }
    }
}
