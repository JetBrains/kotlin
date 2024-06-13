/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.BuildSPMSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.MergeStaticLibrariesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwiftExportUnitTests {
    @Test
    fun `test swift export compilation`() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
        val project = swiftExportProject()
        project.evaluate()

        val compilations = project.multiplatformExtension.iosSimulatorArm64().compilations
        val mainCompilation = compilations.main
        val swiftExportCompilation = compilations.swiftExport

        val mainCompileTask = mainCompilation.compileTaskProvider.get() as KotlinNativeCompile
        val swiftExportCompileTask = swiftExportCompilation.compileTaskProvider.get() as KotlinNativeCompile

        // Main compilation exist
        assertNotNull(mainCompilation)

        // swiftExportMain compilation exist
        assertNotNull(swiftExportCompilation)

        val swiftExportMainAssociation = swiftExportCompilation.associatedCompilations.single()

        // swiftExportMain associated with main
        assertEquals(swiftExportMainAssociation, mainCompilation)

        val mainKlib = mainCompileTask.outputFile.get()
        val swiftExportFriendPaths = swiftExportCompileTask.compilation.friendPaths.files

        // Swift Export compilation have dependency on main compilation
        assert(swiftExportFriendPaths.contains(mainKlib)) { "Swift Export compile task doesn't contain dependency on main klib" }

        val swiftExportLibraries = swiftExportCompileTask.libraries.files

        // Swift Export libraries contains main klib
        assert(swiftExportLibraries.contains(mainKlib))
    }

    @Test
    fun `test swift export embed and sign`() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
        val project = swiftExportProject()
        project.evaluate()

        val swiftExportTask = project.tasks.getByName("iosSimulatorArm64DebugSwiftExport")
        val generateSPMPackageTask = project.tasks.getByName("iosSimulatorArm64DebugGenerateSPMPackage")
        val buildSPMPackageTask = project.tasks.getByName("iosSimulatorArm64DebugBuildSPMPackage")
        val linkSwiftExportBinaryTask = project.tasks.getByName("linkSwiftExportBinaryDebugStaticIosSimulatorArm64")
        val mergeLibrariesTask = project.tasks.getByName("mergeIosSimulatorDebugSwiftExportLibraries")
        val copySwiftExportTask = project.tasks.getByName("copyDebugSPMIntermediates")
        val embedAndSignTask = project.tasks.getByName("embedAndSignAppleFrameworkForXcode")

        // Check embedAndSign task dependencies
        val embedAndSignTaskDependencies = embedAndSignTask.taskDependencies.getDependencies(null)
        assert(embedAndSignTaskDependencies.contains(copySwiftExportTask))

        // Check copySwiftExport task dependencies
        val copySwiftExportTaskDependencies = copySwiftExportTask.taskDependencies.getDependencies(null)
        assert(copySwiftExportTaskDependencies.contains(generateSPMPackageTask))
        assert(copySwiftExportTaskDependencies.contains(buildSPMPackageTask))
        assert(copySwiftExportTaskDependencies.contains(mergeLibrariesTask))

        // Check mergeLibraries task dependencies
        val mergeLibrariesTaskDependencies = mergeLibrariesTask.taskDependencies.getDependencies(null)
        assert(mergeLibrariesTaskDependencies.contains(linkSwiftExportBinaryTask))
        assert(mergeLibrariesTaskDependencies.contains(buildSPMPackageTask))

        // Check buildSPMPackage task dependencies
        val buildSPMPackageTaskDependencies = buildSPMPackageTask.taskDependencies.getDependencies(null)
        assert(buildSPMPackageTaskDependencies.contains(generateSPMPackageTask))

        // Check generateSPMPackage task dependencies
        val generateSPMPackageTaskDependencies = generateSPMPackageTask.taskDependencies.getDependencies(null)
        assert(generateSPMPackageTaskDependencies.contains(swiftExportTask))
    }

    @Test
    fun `test swift export missing arch`() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
        val project = swiftExportProject(archs = "arm64", targets = {
            listOf(iosSimulatorArm64(), iosX64(), iosArm64()).forEach {
                it.binaries.framework()
            }
        })

        project.evaluate()

        val embedAndSignTask = project.tasks.getByName("embedAndSignAppleFrameworkForXcode")

        val buildType = embedAndSignTask.inputs.properties["type"] as NativeBuildType

        val arm64SimLib = project.multiplatformExtension.iosSimulatorArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        val arm64Lib = project.multiplatformExtension.iosArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        val x64Lib = project.multiplatformExtension.iosX64().binaries.findStaticLib("SwiftExportBinary", buildType)

        assertNotNull(arm64SimLib)
        assertNull(arm64Lib)
        assertNull(x64Lib)

        val mergeTask = project.tasks.withType(MergeStaticLibrariesTask::class.java).single()
        val linkTask = mergeTask.taskDependencies.getDependencies(null).filterIsInstance<KotlinNativeLink>().single()

        assertEquals(linkTask.konanTarget, arm64SimLib.konanTarget)
    }

    @Test
    fun `test swift export embed and sign inputs`() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
        val project = swiftExportProject(archs = "arm64 x86_64", targets = {
            listOf(iosSimulatorArm64(), iosX64()).forEach {
                it.binaries.framework()
            }
        })

        project.evaluate()

        val embedAndSignTask = project.tasks.getByName("embedAndSignAppleFrameworkForXcode")

        @Suppress("UNCHECKED_CAST")
        val targets = embedAndSignTask.inputs.properties["targets"] as List<KonanTarget>
        val buildType = embedAndSignTask.inputs.properties["type"] as NativeBuildType

        assert(targets.contains(KonanTarget.IOS_SIMULATOR_ARM64)) { "Targets doesn't contain ios_simulator_arm64" }
        assert(targets.contains(KonanTarget.IOS_X64)) { "Targets doesn't contain ios_x64" }

        assertEquals(buildType, NativeBuildType.DEBUG, "Not a DEBUG configuration")

        val appleTarget = targets.map { it.appleTarget }.distinct().single()

        assertEquals(appleTarget, AppleTarget.IPHONE_SIMULATOR, "Target is not iOS Simulator")

        val swiftExportTasks = project.tasks.withType(SwiftExportTask::class.java)
        val buildSPMTasks = project.tasks.withType(BuildSPMSwiftExportPackage::class.java)
        val buildTypeName = buildType.getName()
        val iosArm64Prefix = "iosSimulatorArm64"
        val iosX64Prefix = "iosX64"

        fun swiftExportExpectedTaskName(prefix: String): String = lowerCamelCaseName(
            prefix,
            buildTypeName,
            "swiftExport"
        )

        // Swift Export should be registered
        assertEquals(
            swiftExportTasks.map { it.name }.first { it.startsWith(iosArm64Prefix) },
            swiftExportExpectedTaskName(iosArm64Prefix),
            "Swift Export task name doesn't match expected prefix $iosArm64Prefix"
        )

        assertEquals(
            swiftExportTasks.map { it.name }.first { it.startsWith(iosX64Prefix) },
            swiftExportExpectedTaskName(iosX64Prefix),
            "Swift Export task name doesn't match expected prefix $iosX64Prefix"
        )

        fun buildSPMTaskName(prefix: String): String = lowerCamelCaseName(
            prefix,
            buildTypeName,
            "BuildSPMPackage"
        )

        assertEquals(
            buildSPMTasks.map { it.name }.first { it.startsWith(iosArm64Prefix) },
            buildSPMTaskName(iosArm64Prefix),
            "Build SPM task name doesn't match expected prefix $iosArm64Prefix"
        )

        assertEquals(
            buildSPMTasks.map { it.name }.first { it.startsWith(iosX64Prefix) },
            buildSPMTaskName(iosX64Prefix),
            "Build SPM task name doesn't match expected prefix $iosX64Prefix"
        )
    }
}

private fun swiftExportProject(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    targets: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64().binaries.framework() },
): ProjectInternal = buildProjectWithMPP(
    preApplyCode = {
        applyEmbedAndSignEnvironment(
            configuration = configuration,
            sdk = sdk,
            archs = archs,
        )
        enableSwiftExport()
    },
    code = {
        repositories.mavenLocal()
        kotlin { targets() }
    }
)

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")