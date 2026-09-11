/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportConstants
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.unevaluatedExportDslProject
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails

class SwiftPackageOutputDirectoryCheckerTest {

    @BeforeTest
    fun runOnMacOSOnly() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
    }

    /**
     * @param outputDirectory the value of `swiftPackageIntegration.outputDirectory`, relative to the project
     * directory, or `null` to leave it unset
     */
    private fun packageProject(
        outputDirectory: String?,
        prepare: File.() -> Unit = {},
    ): ProjectInternal = unevaluatedExportDslProject {
        exportExtension.swift {
            moduleName.set("Shared")
            swiftPackageIntegration {
                if (outputDirectory != null) {
                    this.outputDirectory.set(layout.projectDirectory.dir(outputDirectory))
                }
            }
        }
        projectDir.prepare()
    }

    @Test
    fun `test an unset output directory is reported`() {
        val project = packageProject(outputDirectory = null)

        assertFails { project.evaluate() }
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryNotSet)
    }

    @Test
    fun `test the project directory is refused as the output directory`() {
        val project = packageProject(outputDirectory = ".")

        assertFails { project.evaluate() }
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryUnsafe)
    }

    @Test
    fun `test an output directory holding an xcode project is refused`() {
        val project = packageProject(outputDirectory = "iosApp") {
            resolve("iosApp/Foo.xcodeproj").mkdirs()
        }

        assertFails { project.evaluate() }
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryUnsafe)
    }

    @Test
    fun `test a non empty destination without the marker is refused`() {
        val project = packageProject(outputDirectory = "iosApp/SharedPackage") {
            resolve("iosApp/SharedPackage/Debug").mkdirs()
            resolve("iosApp/SharedPackage/Debug/MyApp.swift").writeText("// hand-written\n")
        }

        assertFails { project.evaluate() }
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryNotEmpty)
    }

    @Test
    fun `test a non empty destination with the marker is accepted`() {
        val project = packageProject(outputDirectory = "iosApp/SharedPackage") {
            resolve("iosApp/SharedPackage/Debug").mkdirs()
            resolve("iosApp/SharedPackage/Debug/Package.swift").writeText("// generated\n")
            resolve("iosApp/SharedPackage/Debug/${SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_NAME}")
                .writeText(SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_CONTENT)
        }

        project.evaluate()
        project.assertNoDiagnostics()
    }

    @Test
    fun `test an empty or absent destination is accepted`() {
        val project = packageProject(outputDirectory = "iosApp/SharedPackage") {
            resolve("iosApp/SharedPackage/Debug").mkdirs()
        }

        project.evaluate()
        project.assertNoDiagnostics()
    }
}
