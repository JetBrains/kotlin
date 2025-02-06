/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroups
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.blue
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.bold
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.green
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.italic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.lightBlue
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.orange
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.red
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.yellow
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.plain
import org.jetbrains.kotlin.gradle.plugin.diagnostics.styled
import kotlin.test.*

class ToolingDiagnosticOutputTest {

    @Test
    fun `test plain diagnostic with warning severity`() {
        val diagnostic = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID("KT-1234", "Test Diagnostic", DiagnosticGroups.KGP.Deprecation),
            message = "Detailed error description",
            severity = ToolingDiagnostic.Severity.WARNING,
            solutions = listOf("Try this fix"),
        )

        val plainOutput = diagnostic.plain(showEmoji = true)

        assertEquals("⚠️ Test Diagnostic", plainOutput.name)
        assertEquals("Detailed error description", plainOutput.message)
        assertEquals("Solution: Try this fix", plainOutput.solution)
        assertNull(plainOutput.documentation)
    }

    @Test
    fun `test styled diagnostic with error severity`() {
        val diagnostic = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID("KT-5678", "Sample Diagnostic", DiagnosticGroups.KGP.Misconfiguration),
            message = "Test error message",
            severity = ToolingDiagnostic.Severity.ERROR,
            solutions = listOf("First solution", "Second solution"),
        )

        val styledOutput = diagnostic.styled(showEmoji = true)

        assertEquals("❌ Sample Diagnostic".bold().red(), styledOutput.name)
        assertEquals("Test error message", styledOutput.message)
        assertEquals(
            buildString {
                appendLine("Solutions:".bold().green())
                appendLine(" • ${"First solution".italic()}".green())
                append(" • ${"Second solution".italic()}".green())
            },
            styledOutput.solution
        )
    }

    @Test
    fun `test styled diagnostic with code blocks`() {
        val messageWithCode = """
            Invalid configuration found:
            ```
            kotlinOptions {
                jvmTarget = "1.8"
            }
            ```
            Please update to a newer version.
        """.trimIndent()

        val diagnostic = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID("KT-9999", "Minimal Test", DiagnosticGroups.KGP.Experimental),
            message = messageWithCode,
            severity = ToolingDiagnostic.Severity.WARNING,
            solutions = listOf("Fix the code")
        )

        val styledOutput = diagnostic.styled(showEmoji = true)

        assertEquals("⚠️ Minimal Test".bold().yellow(), styledOutput.name)
        assertEquals(buildString {
            appendLine("Invalid configuration found:")
            appendLine("kotlinOptions {".orange())
            appendLine("    jvmTarget = \"1.8\"".orange())
            appendLine("}".orange())
            append("Please update to a newer version.")
        }, styledOutput.message)
        assertEquals(
            buildString {
                appendLine("Solution:".bold().green())
                append("Fix the code".italic().green())
            },
            styledOutput.solution
        )
    }

    @Test
    fun `test plain diagnostic without emoji`() {
        val diagnostic = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID("KT-9999", "Minimal Test", DiagnosticGroups.KGP.Misconfiguration),
            message = "Critical error occurred",
            severity = ToolingDiagnostic.Severity.FATAL,
            solutions = listOf("Fix the code")
        )

        val plainOutput = diagnostic.plain(showEmoji = false)

        assertEquals("Minimal Test", plainOutput.name)
        assertEquals("Critical error occurred", plainOutput.message)
        assertEquals("Solution: Fix the code", plainOutput.solution)
    }

    @Test
    fun `test styled diagnostic with documentation`() {
        val diagnostic = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID("KT-9999", "Minimal Test", DiagnosticGroups.KGP.Misconfiguration),
            message = "Configuration warning",
            severity = ToolingDiagnostic.Severity.WARNING,
            solutions = listOf("Fix the code"),
            documentation = ToolingDiagnostic.Documentation(
                url = "https://kotlinlang.org/docs/home.html",
                additionalUrlContext = "See the Kotlin documentation at https://kotlinlang.org/docs/home.html for more details"
            )
        )

        val styledOutput = diagnostic.styled(showEmoji = true)

        assertEquals("⚠️ Minimal Test".bold().yellow(), styledOutput.name)
        assertEquals("Configuration warning", styledOutput.message)
        assertEquals(
            buildString {
                appendLine("Solution:".bold().green())
                append("Fix the code".italic().green())
            },
            styledOutput.solution
        )
        assertEquals(
            buildString {
                append("See the Kotlin documentation at ".lightBlue())
                append("https://kotlinlang.org/docs/home.html".blue())
                append(" for more details".lightBlue())
            },
            styledOutput.documentation
        )
    }
}