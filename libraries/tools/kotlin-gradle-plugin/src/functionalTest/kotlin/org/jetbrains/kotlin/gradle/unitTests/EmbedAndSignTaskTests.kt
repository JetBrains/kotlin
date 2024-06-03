/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SymbolicLinkToFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.assertDependsOn
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbedAndSignTaskTests {

    @Before
    fun runOnlyOnMacOS() {
        Assume.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `assemble framework task - link framework task relationship - when building project for a single architecture`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64",
        ).evaluate()

        val assembleFrameworkTask = assertIsInstance<FrameworkCopy>(
            project.tasks.getByName("assembleDebugAppleFrameworkForXcodeIosSimulatorArm64")
        )
        val linkFrameworkTask = assertIsInstance<KotlinNativeLink>(
            project.tasks.getByName("linkDebugFrameworkIosSimulatorArm64")
        )

        assembleFrameworkTask.assertDependsOn(linkFrameworkTask)

        assertEquals(
            linkFrameworkTask.outputFile.get(),
            project.layout.buildDirectory.file("bin/iosSimulatorArm64/debugFramework/Foo.framework").getFile(),
        )
        assertEquals(
            linkFrameworkTask.outputFile.get(),
            assembleFrameworkTask.sourceFramework.asFile.get(),
        )
    }

    @Test
    fun `assemble framework task - assemble framework task relationship - when building project for a multiple architectures`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64 x86_64",
        ).evaluate()

        val assembleFrameworkTask = assertIsInstance<FatFrameworkTask>(
            project.tasks.getByName("assembleDebugAppleFrameworkForXcode")
        )
        val linkFrameworkTaskArm = assertIsInstance<KotlinNativeLink>(
            project.tasks.getByName("linkDebugFrameworkIosSimulatorArm64")
        )
        val linkFrameworkTaskIntel = assertIsInstance<KotlinNativeLink>(
            project.tasks.getByName("linkDebugFrameworkIosX64")
        )

        assembleFrameworkTask.assertDependsOn(linkFrameworkTaskArm)
        assembleFrameworkTask.assertDependsOn(linkFrameworkTaskIntel)

        assertEquals(
            linkFrameworkTaskArm.outputFile.get(),
            project.layout.buildDirectory.file("bin/iosSimulatorArm64/debugFramework/Foo.framework").getFile(),
        )
        assertEquals(
            linkFrameworkTaskIntel.outputFile.get(),
            project.layout.buildDirectory.file("bin/iosX64/debugFramework/Foo.framework").getFile(),
        )
        assertEquals(
            listOf(
                project.layout.buildDirectory.file("bin/iosSimulatorArm64/debugFramework/Foo.framework").getFile(),
                project.layout.buildDirectory.file("bin/iosX64/debugFramework/Foo.framework").getFile(),
            ),
            assembleFrameworkTask.frameworks.map { it.file },
        )
    }

    @Test
    fun `symbolic link task - assemble framework task relationship - when building project for a single architecture`() {
        val builtProductsDirectory = "/dd"
        val project = embedAndSignProjectWithUniversalTargetCombination(
            builtProductsDirectory = builtProductsDirectory,
            archs = "arm64",
        ).evaluate()

        val assembleFrameworkTask = assertIsInstance<FrameworkCopy>(
            project.tasks.getByName("assembleDebugAppleFrameworkForXcodeIosSimulatorArm64")
        )
        val symbolicLinkTask = assertIsInstance<SymbolicLinkToFrameworkTask>(
            project.tasks.getByName("symbolicLinkToAssembleDebugAppleFrameworkForXcodeIosSimulatorArm64")
        )

        assembleFrameworkTask.assertDependsOn(symbolicLinkTask)

        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework").getFile(),
            assembleFrameworkTask.destinationFramework,
        )
        assertEquals(
            assembleFrameworkTask.destinationFramework,
            symbolicLinkTask.frameworkPath.get(),
        )
        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework.dSYM").getFile(),
            assembleFrameworkTask.destinationDsym,
        )
        assertEquals(
            assembleFrameworkTask.destinationDsym,
            symbolicLinkTask.dsymPath.get(),
        )
        assertEquals(
            File(builtProductsDirectory).resolve("Foo.framework"),
            symbolicLinkTask.frameworkSymbolicLinkPath,
        )
        assertEquals(
            File(builtProductsDirectory).resolve("Foo.framework.dSYM"),
            symbolicLinkTask.dsymSymbolicLinkPath,
        )
    }

    @Test
    fun `symbolic link task - symbolic link task relationship - when building project for multiples architectures`() {
        val builtProductsDirectory = "/dd"
        val project = embedAndSignProjectWithUniversalTargetCombination(
            builtProductsDirectory = builtProductsDirectory,
            archs = "arm64 x86_64",
        ).evaluate()

        val assembleFrameworkTask = assertIsInstance<FatFrameworkTask>(
            project.tasks.getByName("assembleDebugAppleFrameworkForXcode")
        )
        val symbolicLinkTask = assertIsInstance<SymbolicLinkToFrameworkTask>(
            project.tasks.getByName("symbolicLinkToAssembleDebugAppleFrameworkForXcode")
        )

        assembleFrameworkTask.assertDependsOn(symbolicLinkTask)

        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework").getFile(),
            assembleFrameworkTask.fatFramework,
        )
        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework.dSYM").getFile(),
            assembleFrameworkTask.frameworkLayout.dSYM.rootDir,
        )
        assertEquals(
            assembleFrameworkTask.fatFramework,
            symbolicLinkTask.frameworkPath.get(),
        )
        assertEquals(
            assembleFrameworkTask.frameworkLayout.dSYM.rootDir,
            symbolicLinkTask.dsymPath.get(),
        )
        assertEquals(
            File(builtProductsDirectory).resolve("Foo.framework"),
            symbolicLinkTask.frameworkSymbolicLinkPath,
        )
        assertEquals(
            File(builtProductsDirectory).resolve("Foo.framework.dSYM"),
            symbolicLinkTask.dsymSymbolicLinkPath,
        )
    }

    @Test
    fun `symbolic link task - dSYM existance flag depends on framework linkage`() {
        assertFalse(
            assertIsInstance<SymbolicLinkToFrameworkTask>(
                embedAndSignProjectWithUniversalTargetCombination(
                    archs = "arm64",
                    isFrameworkStatic = true
                ).evaluate().tasks.getByName(
                    "symbolicLinkToAssembleDebugAppleFrameworkForXcodeIosSimulatorArm64"
                )
            ).shouldDsymLinkExist.get()
        )

        assertTrue(
            assertIsInstance<SymbolicLinkToFrameworkTask>(
                embedAndSignProjectWithUniversalTargetCombination(
                    archs = "arm64",
                    isFrameworkStatic = false
                ).evaluate().tasks.getByName(
                    "symbolicLinkToAssembleDebugAppleFrameworkForXcodeIosSimulatorArm64"
                )
            ).shouldDsymLinkExist.get()
        )
    }

    private fun embedAndSignProjectWithUniversalTargetCombination(
        archs: String,
        builtProductsDirectory: String = "/dd",
        isFrameworkStatic: Boolean? = null,
    ): ProjectInternal {
        return buildProjectWithMPP(
            preApplyCode = {
                applyEmbedAndSignEnvironment(
                    configuration = "Debug",
                    sdk = "iphonesimulator",
                    archs = archs,
                    builtProductsDirectory = builtProductsDirectory,
                )
            }
        ) {
            kotlin {
                listOf(iosSimulatorArm64(), iosX64()).forEach {
                    it.binaries.framework {
                        baseName = "Foo"
                        isFrameworkStatic?.let {
                            isStatic = it
                        }
                    }
                }
            }
        }
    }

}

