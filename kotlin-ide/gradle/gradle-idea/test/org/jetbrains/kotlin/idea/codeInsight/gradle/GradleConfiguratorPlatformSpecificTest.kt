/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test

class GradleConfiguratorPlatformSpecificTest : KotlinGradleImportingTestCase() {
    @TargetVersions("4.7+")
    @Test
    fun testEnableFeatureSupportMultiplatform() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeFeatureConfiguration(
                    myTestFixture.module, LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED, false
                )
            }

            checkFiles(files)
        }
    }

    @Test
    @TargetVersions("4.7+")
    fun testEnableFeatureSupportMultiplatformWithXFlag() = testEnableFeatureSupportMultiplatform()

    @Test
    @TargetVersions("4.7+")
    fun testEnableFeatureSupportMultiplatform2() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeFeatureConfiguration(
                    myTestFixture.module, LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED, false
                )
            }

            checkFiles(files)
        }
    }

    @Test
    @TargetVersions("4.7+")
    fun testEnableFeatureSupportMultiplatformToExistentArguments() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeFeatureConfiguration(
                    myTestFixture.module, LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED, false
                )
            }

            checkFiles(files)
        }
    }

    @Test
    @TargetVersions("4.7+")
    fun testEnableFeatureSupportMultiplatformKts() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.changeFeatureConfiguration(
                    myTestFixture.module, LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED, false
                )
            }

            checkFiles(files)
        }
    }

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatform() = doTestAddLibrary()

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatformGSK() = doTestAddLibrary()

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatformGSK2() = doTestAddLibrary()

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatformGSK3() = doTestAddLibrary()

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatformGSK4() = doTestAddLibrary()

    @Test
    @TargetVersions("4.7+")
    fun testAddLibraryMultiplatformGSK5() = doTestAddLibrary()

    private fun doTestAddLibrary() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            myTestFixture.project.executeWriteCommand("") {
                KotlinWithGradleConfigurator.addKotlinLibraryToModule(
                    object : Module by myTestFixture.module {
                        override fun getName(): String = "jvmMain"
                    },
                    DependencyScope.COMPILE,
                    object : ExternalLibraryDescriptor("org.jetbrains.kotlin", "kotlin-reflect", "1.3.50", "1.3.50") {
                        override fun getLibraryClassesRoots() = emptyList<String>()
                    })
            }

            checkFiles(files)
        }
    }

    override fun testDataDirName(): String {
        return "configurator"
    }
}