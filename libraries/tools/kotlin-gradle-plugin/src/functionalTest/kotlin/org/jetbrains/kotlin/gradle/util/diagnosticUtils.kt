/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

internal fun checkDiagnosticsWithMppProject(projectName: String, projectConfiguration: Project.() -> Unit) {
    val project = buildProjectWithMPP(projectBuilder = { withName(projectName) })
    project.setMultiplatformAndroidSourceSetLayoutVersion(2)
    project.projectConfiguration()
    project.evaluate()
    project.checkDiagnostics(projectName)
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

private fun Collection<ToolingDiagnostic>.render(): String = joinToString(separator = "\n----\n")

private val expectedDiagnosticsRoot: Path
    get() = resourcesRoot.resolve("expectedDiagnostics")

private fun expectedDiagnosticsFile(projectName: String): File = expectedDiagnosticsRoot.resolve("$projectName.txt").toFile()
