/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.CopyDsymDuringArchiving
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
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbedAndSignTaskTests {
    @get:Rule val muteableTestRule = MuteableTestRule()

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

    @Test
    fun `symbolic link task - dSYM shouldn't exist when archiving`() {
        assertFalse(
            assertIsInstance<SymbolicLinkToFrameworkTask>(
                embedAndSignProjectWithUniversalTargetCombination(
                    archs = "arm64",
                    isFrameworkStatic = false,
                    action = "install"
                ).evaluate().tasks.getByName(
                    "symbolicLinkToAssembleDebugAppleFrameworkForXcodeIosSimulatorArm64"
                )
            ).shouldDsymLinkExist.get()
        )
    }

    @Test
    fun `dward dsym copy task - is created disabled by default`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64",
            dwarfDsymFolderPath = null,
            isFrameworkStatic = false,
        ).evaluate()
        val task = assertIsInstance<CopyDsymDuringArchiving>(
            project.tasks.getByName("copyDsymForEmbedAndSignAppleFrameworkForXcode")
        )
        assertEquals(
            false,
            task.onlyIf.isSatisfiedBy(task),
        )
        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework.dSYM").getFile(),
            task.dsymPath.orNull,
        )
        assertEquals(
            null,
            task.dwarfDsymFolderPath.orNull,
        )
    }

    @Test
    fun `dward dsym copy task - is enabled by archive action`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64",
            dwarfDsymFolderPath = "/path/to/dsym_folder",
            isFrameworkStatic = false,
            action = "install",
        ).evaluate()
        val task = assertIsInstance<CopyDsymDuringArchiving>(
            project.tasks.getByName("copyDsymForEmbedAndSignAppleFrameworkForXcode")
        )
        assertEquals(
            true,
            task.onlyIf.isSatisfiedBy(task),
        )
        assertEquals(
            project.layout.buildDirectory.file("xcode-frameworks/Debug/iphonesimulator/Foo.framework.dSYM").getFile(),
            task.dsymPath.orNull,
        )
        assertEquals(
            "/path/to/dsym_folder",
            task.dwarfDsymFolderPath.orNull,
        )
    }

    @Test
    fun `dward dsym copy task - is disabled for static framework`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64",
            dwarfDsymFolderPath = "/path/to/dsym_folder",
            isFrameworkStatic = true,
            action = "install",
        ).evaluate()
        val task = assertIsInstance<CopyDsymDuringArchiving>(
            project.tasks.getByName("copyDsymForEmbedAndSignAppleFrameworkForXcode")
        )
        assertEquals(
            false,
            task.onlyIf.isSatisfiedBy(task),
        )
    }


    @Test
    fun `dward dsym copy task - has an explicit dependecy on symbolic link task - because symbolic link task does clean-up for KT-68257`() {
        val project = embedAndSignProjectWithUniversalTargetCombination(
            archs = "arm64",
            dwarfDsymFolderPath = "/path/to/dsym_folder",
            isFrameworkStatic = false,
            action = "install",
        ).evaluate()
        val task = assertIsInstance<CopyDsymDuringArchiving>(
            project.tasks.getByName("copyDsymForEmbedAndSignAppleFrameworkForXcode")
        )
        assert(
            task.taskDependencies.getDependencies(null).contains(
                project.tasks.getByName("symbolicLinkToAssembleDebugAppleFrameworkForXcodeIosSimulatorArm64")
            )
        )
    }

    private fun embedAndSignProjectWithUniversalTargetCombination(
        archs: String,
        builtProductsDirectory: String = "/dd",
        dwarfDsymFolderPath: String? = null,
        isFrameworkStatic: Boolean? = null,
        action: String = "build",
    ): ProjectInternal {
        return buildProjectWithMPP(
            preApplyCode = {
                applyEmbedAndSignEnvironment(
                    configuration = "Debug",
                    sdk = "iphonesimulator",
                    archs = archs,
                    builtProductsDirectory = builtProductsDirectory,
                    dwarfDsymFolderPath = dwarfDsymFolderPath,
                    action = action,
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

