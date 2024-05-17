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
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment.Companion.XCODE_ENVIRONMENT_OVERRIDE_KEY
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        val mergeLibrariesTask = project.tasks.getByName("iosSimulatorArm64DebugMergeLibraries")
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
            listOf(iosSimulatorArm64(), iosX64()).forEach {
                it.binaries.framework()
            }
        })

        project.evaluate()

        val embedAndSignTask = project.tasks.getByName("embedAndSignAppleFrameworkForXcode")

        val buildType = embedAndSignTask.inputs.properties["type"] as NativeBuildType

        val arm64SimFramework = project.multiplatformExtension.iosSimulatorArm64().binaries.getFramework(buildType)
        val x64Framework = project.multiplatformExtension.iosX64().binaries.getFramework(buildType)

        val arm64SimTargetName = arm64SimFramework.targetName
        val x64TargetName = x64Framework.targetName
        val buildTypeName = buildType.getName()

        val swiftExportTasks = project.tasks.withType(SwiftExportTask::class.java).map { it.name }

        val arm64SimExpectedTaskName = lowerCamelCaseName(
            arm64SimTargetName,
            buildTypeName,
            "swiftExport"
        )

        val x64ExpectedTaskName = lowerCamelCaseName(
            x64TargetName,
            buildTypeName,
            "swiftExport"
        )

        // Swift Export should be registered only for arm64
        assert(swiftExportTasks.contains(arm64SimExpectedTaskName)) { "Doesn't contain $arm64SimExpectedTaskName" }
        assert(swiftExportTasks.contains(x64ExpectedTaskName).not()) { "Contains $x64ExpectedTaskName" }
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

        val swiftExportTasks = project.tasks.withType(SwiftExportTask::class.java).map { it.name }

        val arm64SimFramework = project.multiplatformExtension.iosSimulatorArm64().binaries.getFramework(buildType)
        val x64Framework = project.multiplatformExtension.iosX64().binaries.getFramework(buildType)

        val arm64SimTargetName = arm64SimFramework.targetName
        val x64TargetName = x64Framework.targetName
        val buildTypeName = buildType.getName()

        val arm64SimExpectedTaskName = lowerCamelCaseName(
            arm64SimTargetName,
            buildTypeName,
            "swiftExport"
        )

        val x64ExpectedTaskName = lowerCamelCaseName(
            x64TargetName,
            buildTypeName,
            "swiftExport"
        )

        // Swift Export should be registered for both arm64 and x64 targets
        assert(swiftExportTasks.contains(arm64SimExpectedTaskName)) { "Doesn't contain $arm64SimExpectedTaskName" }
        assert(swiftExportTasks.contains(x64ExpectedTaskName)) { "Doesn't contain $x64ExpectedTaskName" }
    }
}

private fun swiftExportProject(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    targets: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64().binaries.framework() }
): ProjectInternal = buildProjectWithMPP(
    preApplyCode = {
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.CONFIGURATION",
            configuration
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.SDK_NAME",
            sdk
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.ARCHS",
            archs
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.BUILT_PRODUCTS_DIR",
            layout.buildDirectory.dir("products").getFile().canonicalPath
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.TARGET_BUILD_DIR",
            layout.buildDirectory.dir("buildDir").getFile().canonicalPath
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.FRAMEWORKS_FOLDER_PATH",
            "Frameworks"
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.EXPANDED_CODE_SIGN_IDENTITY",
            "-"
        )
        project.extensions.extraProperties.set(
            "$XCODE_ENVIRONMENT_OVERRIDE_KEY.ENABLE_USER_SCRIPT_SANDBOXING",
            "NO"
        )

        enableSwiftExport()
    },
    code = {
        repositories.mavenLocal()
        kotlin { targets() }
    }
)

private val Framework.targetName get() = target.let { it.disambiguationClassifier ?: it.name }

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")