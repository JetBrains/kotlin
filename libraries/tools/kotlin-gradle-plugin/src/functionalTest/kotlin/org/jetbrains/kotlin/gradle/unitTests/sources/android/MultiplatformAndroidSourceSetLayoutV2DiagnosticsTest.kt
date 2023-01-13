/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker.AndroidStyleSourceDirUsageDiagnostic
import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.MultiplatformLayoutV2MultiplatformLayoutV1StyleSourceDirUsageChecker.V1StyleSourceDirUsageDiagnostic
import org.jetbrains.kotlin.gradle.plugin.sources.android.findAndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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

    @Test
    fun `test - android style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject()
        val androidStyleMain = project.file("src/main/kotlin")
        val androidStyleUnitTest = project.file("src/test/kotlin")

        androidStyleMain.mkdirs()
        androidStyleUnitTest.mkdirs()
        project.evaluate()

        project.checkCreatedSourceSets()

        val warnings = diagnosticsReporter.warnings
        if (warnings.size != 2) fail("Expected exactly two warnings emitted. Found $warnings")

        val androidMainWarning = warnings.filterIsInstance<AndroidStyleSourceDirUsageDiagnostic>()
            .find { warning -> warning.androidStyleSourceDirInUse == androidStyleMain }
            ?: fail("Missing warning for '$androidStyleMain'. Found $warnings")

        assertEquals(
            project.file("src/androidMain/kotlin"), androidMainWarning.kotlinStyleSourceDirToUse
        )

        val androidUnitTestWarning = warnings.filterIsInstance<AndroidStyleSourceDirUsageDiagnostic>()
            .find { warning -> warning.androidStyleSourceDirInUse == androidStyleUnitTest }
            ?: fail("Missing warning for '$androidStyleUnitTest'. Found $warnings")

        assertEquals(
            project.file("src/androidUnitTest/kotlin"), androidUnitTestWarning.kotlinStyleSourceDirToUse
        )
    }

    @Test
    fun `test - nowarn flag - android style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject()
        project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN, "true")
        val androidStyleMain = project.file("src/main/kotlin")
        val androidStyleUnitTest = project.file("src/test/kotlin")
        val androidStyleInstrumentedTest = project.file("src/androidTest/kotlin")

        androidStyleMain.mkdirs()
        androidStyleUnitTest.mkdirs()
        androidStyleInstrumentedTest.mkdirs()
        project.evaluate()

        project.checkCreatedSourceSets()

        val warnings = diagnosticsReporter.warnings
        if (warnings.isNotEmpty()) fail("Expected no warnings emitted. Found $warnings")
    }

    @Test
    fun `test - v1 style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject()
        val v1StyleInstrumentedTest = project.file("src/androidAndroidTest/kotlin")
        v1StyleInstrumentedTest.mkdirs()
        project.evaluate()
        project.checkCreatedSourceSets()

        val warning = assertIsInstance<V1StyleSourceDirUsageDiagnostic>(diagnosticsReporter.assertSingleWarning())

        assertEquals(
            v1StyleInstrumentedTest, warning.v1StyleSourceDirInUse,
        )

        assertEquals(
            project.file("src/androidInstrumentedTest/kotlin"), warning.v2StyleSourceDirToUse
        )
    }
}