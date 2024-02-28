/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.ProjectLayout
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.EmbedAndSignTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironmentContainer.XCODE_ENVIRONMENT_KEY
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.BuildSyntheticProjectWithSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.CopySwiftExportIntermediatesForConsumer
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.GenerateSPMPackageFromSwiftExport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableSwiftExport
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull

class SwiftExportUnitTests {
    @Test
    fun `swift export compilation test`() {
        Assume.assumeTrue("Macos host required for this test", HostManager.hostIsMac)
        val project = buildProjectWithMPP {
            project.extraProperties.set(XCODE_ENVIRONMENT_KEY, TestXcodeEnvironment(project.layout))
            project.multiplatformExtension.iosSimulatorArm64()
            project.enableSwiftExport(true)

            kotlin {
                iosSimulatorArm64 {
                    binaries.framework {
                        baseName = "Shared"
                    }
                }
            }
        }

        project.evaluate()

        // Check compile tasks configured
        assertNotNull(project.tasks.locateTask<KotlinNativeCompile>("compileKotlinIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeCompile>("compileSwiftExportMainKotlinIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeCompile>("compileTestKotlinIosSimulatorArm64"))

        // Check link tasks configured
        assertNotNull(project.tasks.locateTask<KotlinNativeLink>("linkDebugFrameworkIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeLink>("linkDebugTestIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeLink>("linkReleaseFrameworkIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeLink>("linkSwiftExportBinaryDebugStaticIosSimulatorArm64"))
        assertNotNull(project.tasks.locateTask<KotlinNativeLink>("linkSwiftExportBinaryReleaseStaticIosSimulatorArm64"))


        // Framework copy task configured
        assertNotNull(project.tasks.locateTask<FrameworkCopy>("assembleDebugAppleFrameworkForXcodeIosSimulatorArm64"))

        // Swift Export task
        assertNotNull(project.tasks.locateTask<SwiftExportTask>("iosSimulatorArm64DebugSwiftExport"))

        // Generate SPM task
        assertNotNull(project.tasks.locateTask<GenerateSPMPackageFromSwiftExport>("iosSimulatorArm64DebugGenerateSPMPackage"))

        // Build synthetic task
        assertNotNull(project.tasks.locateTask<BuildSyntheticProjectWithSwiftExportPackage>("iosSimulatorArm64DebugBuildSyntheticProject"))

        //Copy Swift Export task
        assertNotNull(project.tasks.locateTask<CopySwiftExportIntermediatesForConsumer>("iosSimulatorArm64DebugCopySyntheticProjectIntermediates"))

        //Embed and Sign task
        assertNotNull(project.tasks.locateTask<EmbedAndSignTask>("embedAndSignAppleFrameworkForXcode"))
    }
}

private class TestXcodeEnvironment(layout: ProjectLayout) : XcodeEnvironment {
    override val buildType: NativeBuildType = NativeBuildType.DEBUG
    override val targets: List<KonanTarget> = listOf(KonanTarget.IOS_SIMULATOR_ARM64)

    override val frameworkSearchDir: File = layout.buildDirectory.dir("frameworks").getFile()
    override val builtProductsDir: File = layout.buildDirectory.dir("products").getFile()
    override val embeddedFrameworksDir: File = layout.buildDirectory.dir("buildDir").getFile()

    override val sign: String = "-"
    override val userScriptSandboxingEnabled: Boolean = false

    override fun toString() = printDebugInfo()
}