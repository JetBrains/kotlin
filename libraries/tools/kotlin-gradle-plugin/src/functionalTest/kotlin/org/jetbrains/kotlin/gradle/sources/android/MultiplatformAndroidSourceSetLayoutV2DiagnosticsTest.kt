/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.sources.android

import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker.AndroidStyleSourceDirUsageDiagnostic
import org.jetbrains.kotlin.gradle.plugin.sources.android.findAndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.junit.Test
import kotlin.test.assertEquals

class MultiplatformAndroidSourceSetLayoutV2DiagnosticsTest {

    private val diagnosticsReporter = TestDiagnosticsReporter()

    private fun buildMinimalAndroidMultiplatformProject(): ProjectInternal = buildProjectWithMPP {
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        plugins.apply(LibraryPlugin::class.java)
        androidLibrary {
            compileSdk = 31
        }
        kotlin {
            android()
        }
    }

    private fun Project.checkCreatedSourceSets(
        diagnosticsReporter: TestDiagnosticsReporter = this@MultiplatformAndroidSourceSetLayoutV2DiagnosticsTest.diagnosticsReporter
    ) {
        /* Invoke checkers on all source sets */
        project.multiplatformExtension.sourceSets.forEach { kotlinSourceSet ->
            val androidSourceSet = project.findAndroidSourceSet(kotlinSourceSet) ?: return@forEach
            multiplatformAndroidSourceSetLayoutV2.checker.checkCreatedSourceSet(
                diagnosticReporter = diagnosticsReporter,
                target = project.multiplatformExtension.android(),
                layout = multiplatformAndroidSourceSetLayoutV2,
                kotlinSourceSet = kotlinSourceSet,
                androidSourceSet = androidSourceSet
            )
        }
    }

    @Test
    fun `test - KT-53709 - androidTest_kotlin in use`() {

        val project = buildMinimalAndroidMultiplatformProject()

        /* Ensure that the problematic androidTest/kotlin source dir is 'in use' */
        val androidTestKotlinSourceDir = project.file("src/androidTest/kotlin")
        androidTestKotlinSourceDir.mkdirs()
        project.evaluate()

        /* Invoke checkers on all source sets */
        project.checkCreatedSourceSets()

        val diagnostic = assertIsInstance<AndroidStyleSourceDirUsageDiagnostic>(diagnosticsReporter.assertSingleWarning())
        assertEquals(androidTestKotlinSourceDir, diagnostic.androidStyleSourceDirInUse)
        assertEquals(project.file("src/androidInstrumentedTest/kotlin"), diagnostic.kotlinStyleSourceDirToUse)
    }
}