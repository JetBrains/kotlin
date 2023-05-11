/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.createAndroidSourceSetLayoutV1SourceSetsNotFoundWarning
import org.jetbrains.kotlin.gradle.util.androidApplication
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

class AndroidSourceSetLayoutV1SourceSetsNotFoundErrorTest {

    @Test
    fun `androidTest SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidTest")
            .assertContainsDiagnosticForSourceSet("androidTest")
    }

    @Test
    fun ` androidAndroidTest SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidAndroidTest")
            .assertContainsDiagnosticForSourceSet("androidAndroidTest")
    }

    @Test
    fun ` androidTestDebug SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidTestDebug")
            .assertContainsDiagnosticForSourceSet("androidTestDebug")
    }

    @Test
    fun `androidAndroidTestDebug SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidAndroidTestDebug")
            .assertContainsDiagnosticForSourceSet("androidAndroidTestDebug")
    }

    @Test
    fun `androidDummy SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidDummy")
            .assertNoDiagnostics()
    }

    @Test
    fun `androidUnitTestX SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidUnitTestX")
            .assertNoDiagnostics()
    }

    private fun diagnosticsForTestProjectRequestingMissingSourceSet(sourceSetName: String): List<String> {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 32 }
        val kotlin = project.multiplatformExtension

        /* Get project into broken state */
        project.afterEvaluate { kotlin.sourceSets.getByName(sourceSetName) }
        assertFails { project.evaluate() }

        /* Test diagnostic in isolation */
        val warnings = mutableListOf<String>()
        project.runAndroidSourceSetLayoutV1SourceSetsNotFoundDiagnostic { warning -> warnings.add(warning) }
        return warnings
    }

    private fun List<String>.assertNoDiagnostics() {
        if (isNotEmpty()) {
            fail("Expected no warning emitted. Found:\n${joinToString("\n") { it.prependIndent("    ") }}")
        }
    }

    private fun List<String>.assertContainsDiagnosticForSourceSet(name: String) {
        if (isEmpty()) fail("Expected diagnostic emitted for SourceSet '$name', but found nothing")
        if (size > 1) fail(
            "Expected single diagnostic emitted for SourceSet '$name', but found multiple:\n" +
                    joinToString("\b") { it.prependIndent("    ") }
        )
        assertEquals(createAndroidSourceSetLayoutV1SourceSetsNotFoundWarning(name), single())
    }
}
