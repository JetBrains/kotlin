/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.sources.android.findAndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test

class MultiplatformAndroidSourceSetLayoutV2DiagnosticsTest {

    private fun buildMinimalAndroidMultiplatformProject(): ProjectInternal = buildProjectWithMPP {
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        plugins.apply(LibraryPlugin::class.java)
        androidLibrary {
            compileSdk = 31
        }
        kotlin {
            androidTarget()
        }
    }

    private fun Project.checkCreatedSourceSets() {
        /* Invoke checkers on all source sets */
        project.multiplatformExtension.sourceSets.forEach { kotlinSourceSet ->
            val androidSourceSet = project.findAndroidSourceSet(kotlinSourceSet) ?: return@forEach
            multiplatformAndroidSourceSetLayoutV2.checker.checkCreatedSourceSet(
                diagnosticsCollector = project.kotlinToolingDiagnosticsCollector,
                target = project.multiplatformExtension.androidTarget(),
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
        project.checkDiagnostics("kt53709AndroidTest_kotlinInUse")
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
        project.checkDiagnostics("androidStyleSourceDirUsage")
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
        project.checkDiagnostics("androidStyleSourceDirUsageNoWarn")
    }

    @Test
    fun `test - v1 style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject()
        val v1StyleInstrumentedTest = project.file("src/androidAndroidTest/kotlin")
        v1StyleInstrumentedTest.mkdirs()
        project.evaluate()
        project.checkCreatedSourceSets()
        project.checkDiagnostics("v1LayoutStyleSourceDirUsage")
    }
}
