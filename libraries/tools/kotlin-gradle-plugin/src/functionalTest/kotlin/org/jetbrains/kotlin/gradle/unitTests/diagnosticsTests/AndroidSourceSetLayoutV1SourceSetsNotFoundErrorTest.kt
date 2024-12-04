/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidSourceSetLayoutV1SourceSetsNotFoundError
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.util.androidApplication
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.assertFails

class AndroidSourceSetLayoutV1SourceSetsNotFoundErrorTest {

    @Test
    fun `androidTest SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidTest")
            .assertContainsDiagnostic(AndroidSourceSetLayoutV1SourceSetsNotFoundError("androidTest"))
    }

    @Test
    fun ` androidAndroidTest SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidAndroidTest")
            .assertContainsDiagnostic(AndroidSourceSetLayoutV1SourceSetsNotFoundError("androidAndroidTest"))
    }

    @Test
    fun ` androidTestDebug SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidTestDebug")
            .assertContainsDiagnostic(AndroidSourceSetLayoutV1SourceSetsNotFoundError("androidTestDebug"))
    }

    @Test
    fun `androidAndroidTestDebug SourceSet`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidAndroidTestDebug")
            .assertContainsDiagnostic(AndroidSourceSetLayoutV1SourceSetsNotFoundError("androidAndroidTestDebug"))
    }

    @Test
    fun `androidDummy SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidDummy")
            .assertNoDiagnostics(AndroidSourceSetLayoutV1SourceSetsNotFoundError)
    }

    @Test
    fun `androidUnitTestX SourceSet - is not reported`() {
        diagnosticsForTestProjectRequestingMissingSourceSet("androidUnitTestX")
            .assertNoDiagnostics(AndroidSourceSetLayoutV1SourceSetsNotFoundError)
    }

    private fun diagnosticsForTestProjectRequestingMissingSourceSet(sourceSetName: String): List<ToolingDiagnostic> {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 32 }
        val kotlin = project.multiplatformExtension

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript) {
            kotlin.sourceSets.getByName(sourceSetName)
        }

        assertFails { project.evaluate() }
        return project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project).toList()
    }
}
