/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.file.ProjectLayout
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironmentContainer.XCODE_ENVIRONMENT_KEY
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.targets
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
            enableSwiftExport(true)
            repositories.mavenLocal()

            with(multiplatformExtension) {
                iosSimulatorArm64 {
                    binaries.framework {
                        baseName = "Shared"
                    }
                }
            }
        }

        project.evaluate()

        val compilations = project.kotlinExtension
            .targets
            .filter { it.name.contains("ios") }
            .map { it.compilations }
            .single()

        val mainCompilation = compilations.main
        val swiftExportCompilation = compilations.swiftExport

        // Main compilation exist
        assertNotNull(mainCompilation)

        // swiftExportMain compilation exist
        assertNotNull(swiftExportCompilation)

        val swiftExportMainAssociation = swiftExportCompilation.associatedCompilations.single()

        // swiftExportMain associated with main
        assertEquals(swiftExportMainAssociation, mainCompilation)
    }
}

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")

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