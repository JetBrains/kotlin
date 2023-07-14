/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue
import kotlin.test.fail

internal fun checkDiagnosticsWithMppProject(projectName: String, projectConfiguration: Project.() -> Unit) {
    val project = buildProjectWithMPP(projectBuilder = { withName(projectName) })
    project.setMultiplatformAndroidSourceSetLayoutVersion(2)
    project.projectConfiguration()
    project.evaluate()
    project.checkDiagnostics(projectName)
}

internal fun ToolingDiagnostic.equals(that: ToolingDiagnostic, ignoreThrowable: Boolean) = if (ignoreThrowable) {
    this.id == that.id && this.message == that.message && this.severity == that.severity
} else {
    this == that
}

/**
 * [compactRendering] == true will omit projects with no diagnostics from the report, as well as
 * name of the project if it's a single one with diagnostics (useful for small one-project tests)
 */
internal fun Project.checkDiagnostics(testDataName: String, compactRendering: Boolean = true) {
    val diagnosticsPerProject = rootProject.allprojects.mapNotNull {
        val diagnostics = it.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(it)
        if (diagnostics.isEmpty() && compactRendering)
            null
        else
            it.name to diagnostics
    }.toMap()
    val expectedDiagnostics = expectedDiagnosticsFile(testDataName)

    if (diagnosticsPerProject.all { (_, diagnostics) -> diagnostics.isEmpty() }) {
        if (expectedDiagnostics.exists())
            error("Expected to have some diagnostics in file://${expectedDiagnostics.canonicalPath}, but none were actually reported")
        else
            return // do not create empty file
    }

    val actualRenderedText = if (diagnosticsPerProject.size == 1 && compactRendering) {
        diagnosticsPerProject.entries.single().value.render()
    } else {
        diagnosticsPerProject
            .entries
            .joinToString(separator = "\n\n") { (projectName, diagnostics) ->
                val nameSanitized = if (projectName == "test") "<root>" else projectName
                "PROJECT: $nameSanitized\n" + diagnostics.render()
            }
    }

    val sanitizedTest = actualRenderedText.replace(File.separator, "/")

    KotlinTestUtils.assertEqualsToFile(expectedDiagnostics, sanitizedTest)
}

internal fun Project.assertNoDiagnostics() {
    val actualDiagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
    assertTrue(
        actualDiagnostics.isEmpty(), "Expected to have no diagnostics, but some were reported:\n ${actualDiagnostics.render()}"
    )
}

/**
 * Checks that diagnostic with [factory.id] is reported. The exact parameters (if any)
 * are ignored. If you need to compare the parameters, refer to the overload accepting [ToolingDiagnostic]
 */
internal fun Project.assertContainsDiagnostic(factory: ToolingDiagnosticFactory) {
    kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this).assertContainsDiagnostic(factory)
}

internal fun Project.assertContainsDiagnostic(diagnostic: ToolingDiagnostic, ignoreThrowable: Boolean = false) {
    kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
        .assertContainsDiagnostic(diagnostic, ignoreThrowable)
}

private fun Any.withIndent() = this.toString().prependIndent("    ")

internal fun Collection<ToolingDiagnostic>.assertContainsDiagnostic(factory: ToolingDiagnosticFactory) {
    if (!any { it.id == factory.id }) failDiagnosticNotFound("diagnostic with id ${factory.id} ", this)
}

internal fun Collection<ToolingDiagnostic>.assertContainsDiagnostic(diagnostic: ToolingDiagnostic, ignoreThrowable: Boolean = false) {
    if (none { it.equals(diagnostic, ignoreThrowable) }) failDiagnosticNotFound("diagnostic $diagnostic\n", this)
}

private fun failDiagnosticNotFound(diagnosticDescription: String, notFoundInCollection: Collection<ToolingDiagnostic>) {
    fail("Missing ${diagnosticDescription}in:\n${notFoundInCollection.render().withIndent()}")
}

internal fun Collection<ToolingDiagnostic>.assertDiagnostics(vararg diagnostics: ToolingDiagnostic) {
    val expectedDiagnostics = diagnostics.toSet()
    val actualDiagnostic = this.toSet()
    if (expectedDiagnostics == actualDiagnostic) return

    val missingDiagnostics = this - expectedDiagnostics
    val unexpectedDiagnostics = expectedDiagnostics - this

    val errorMessage = buildString {
        if (missingDiagnostics.isNotEmpty()) {
            appendLine(missingDiagnostics.joinToString(prefix = "Missing diagnostic\n", separator = "\n") { it.withIndent() })
        }
        if (unexpectedDiagnostics.isNotEmpty()) {
            appendLine(unexpectedDiagnostics.joinToString(prefix = "Unexpected diagnostic\n", separator = "\n") { it.withIndent() })
        }
        appendLine("in: \n${actualDiagnostic.render().withIndent()}")
    }

    fail(errorMessage)
}

internal fun Project.assertNoDiagnostics(factory: ToolingDiagnosticFactory) {
    kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this).assertNoDiagnostics(factory.id)
}

internal fun Collection<ToolingDiagnostic>.assertNoDiagnostics(id: String) {
    val unexpectedDiagnostics = filter { it.id == id }
    if (unexpectedDiagnostics.isNotEmpty()) {
        fail("Expected to have no diagnostics with id '$id', but some were reported:\n${unexpectedDiagnostics.render()}")
    }
}

internal fun Collection<ToolingDiagnostic>.assertNoDiagnostics(factory: ToolingDiagnosticFactory) {
    assertNoDiagnostics(factory.id)
}

private fun Collection<ToolingDiagnostic>.render(): String = joinToString(separator = "\n----\n")

private val expectedDiagnosticsRoot: Path
    get() = resourcesRoot.resolve("expectedDiagnostics")

private fun expectedDiagnosticsFile(projectName: String): File = expectedDiagnosticsRoot.resolve("$projectName.txt").toFile()
