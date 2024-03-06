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
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SwiftExportUnitTests {
    @Test
    fun `swift export compilation test`() {
        Assume.assumeTrue("Macos host required for this test", HostManager.hostIsMac)
        val project = buildProjectWithMPP {
            extraProperties.set(XCODE_ENVIRONMENT_KEY, TestXcodeEnvironment(project.layout))
            multiplatformExtension.iosSimulatorArm64()
            enableSwiftExport(true)
            repositories.mavenLocal()

            kotlin {
                iosSimulatorArm64 {
                    binaries.framework {
                        baseName = "Shared"
                    }
                }
            }
        }

        project.evaluate()

        val compilations = project.multiplatformExtension.iosSimulatorArm64().compilations
        val mainCompilation = compilations.main
        val swiftExportMainCompilation = compilations.getByName("swiftExportMain")

        // Main compilation exist
        assertNotNull(mainCompilation)

        // swiftExportMain compilation exist
        assertNotNull(swiftExportMainCompilation)

        val swiftExportMainAssociation = swiftExportMainCompilation.associatedCompilations.single()

        // swiftExportMain associated with main
        assertEquals(swiftExportMainAssociation, mainCompilation)
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