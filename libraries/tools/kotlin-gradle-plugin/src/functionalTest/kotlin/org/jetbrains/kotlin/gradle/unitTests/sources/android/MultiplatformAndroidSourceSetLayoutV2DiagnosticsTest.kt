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
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertFalse

class MultiplatformAndroidSourceSetLayoutV2DiagnosticsTest {

    private fun buildMinimalAndroidMultiplatformProject(
        preApplyCode: Project.() -> Unit = {}
    ): ProjectInternal = buildProjectWithMPP(preApplyCode = preApplyCode) {
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        plugins.apply(LibraryPlugin::class.java)
        androidLibrary {
            compileSdk = 31
        }
        kotlin {
            @Suppress("DEPRECATION")
            androidTarget()
        }
    }

    @Test
    fun `test - KT-53709 - androidTest_kotlin in use`() {
        val project = buildMinimalAndroidMultiplatformProject {
            /* Ensure that the problematic androidTest/kotlin source dir is 'in use' */
            val androidTestKotlinSourceDir = project.file("src/androidTest/kotlin")
            androidTestKotlinSourceDir.mkdirs()
        }
        project.evaluate()

        project.checkDiagnostics("kt53709AndroidTest_kotlinInUse")
    }

    @Test
    fun `test - android style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject {
            val androidStyleMain = project.file("src/main/kotlin")
            val androidStyleUnitTest = project.file("src/test/kotlin")
            androidStyleMain.mkdirs()
            androidStyleUnitTest.mkdirs()
        }

        project.evaluate()

        project.checkDiagnostics("androidStyleSourceDirUsage")
    }

    @Test
    fun `test - nowarn flag - android style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN, "true")
            val androidStyleMain = project.file("src/main/kotlin")
            val androidStyleUnitTest = project.file("src/test/kotlin")
            val androidStyleInstrumentedTest = project.file("src/androidTest/kotlin")

            androidStyleMain.mkdirs()
            androidStyleUnitTest.mkdirs()
            androidStyleInstrumentedTest.mkdirs()
        }
        project.evaluate()

        project.checkDiagnostics("androidStyleSourceDirUsageNoWarn")
    }

    @Test
    fun `test - v1 style source dir usage checker`() {
        val project = buildMinimalAndroidMultiplatformProject {
            val v1StyleInstrumentedTest = project.file("src/androidAndroidTest/kotlin")
            v1StyleInstrumentedTest.mkdirs()
        }
        project.evaluate()
        project.checkDiagnostics("v1LayoutStyleSourceDirUsage")
    }

    @Test
    fun `KT-78993 - MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker materializes lazy provider on plugin application`() {
        var isSrcDirProviderMaterialized = false
        val project = buildMinimalAndroidMultiplatformProject {
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                pluginManager.withPlugin("com.android.library") {
                    project.multiplatformExtension.sourceSets.all {
                        it.kotlin.srcDir(
                            project.provider {
                                isSrcDirProviderMaterialized = true
                                project.configurations.getByName(
                                    project.multiplatformExtension.sourceSets.getByName("commonMain").implementationConfigurationName
                                ).allDependencies
                                project.files()
                            }
                        )
                    }
                }
            }
        }
        assertFalse(isSrcDirProviderMaterialized, "Provider is not supposed to be materialized at plugin application time")
        project.multiplatformExtension.dependencies {
            implementation.add("foo:bar")
        }
        project.evaluate()
    }
}
