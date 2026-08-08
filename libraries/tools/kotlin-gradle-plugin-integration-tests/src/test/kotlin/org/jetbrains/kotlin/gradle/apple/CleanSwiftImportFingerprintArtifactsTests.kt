/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.encodeToString
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.CleanSwiftImportFingerprintArtifacts
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SHARED_CHECKOUT_DIR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SHARED_SYNTHETIC_PACKAGE_DIR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SHARED_XCODE_DUMP_DIR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprintedCoordinationService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprint
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.fingerprintJson
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryDoesNotExist
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.uklibs.include
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class CleanSwiftImportFingerprintArtifactsTests : KGPBaseTest() {

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `clean removes artifacts shared by projects with the same fingerprints`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            val fingerprints = Fingerprints(packageFingerprint = "shared-package", xcodebuildFingerprint = "shared-xcodebuild")
            val lib1 = cleanupProject(version, fingerprints)
            val lib2 = cleanupProject(version, fingerprints)
            include(lib1, "lib1")
            include(lib2, "lib2")

            val sharedArtifacts = createSharedArtifacts(fingerprints)
            build(":lib1:clean")

            sharedArtifacts.forEach(::assertDirectoryDoesNotExist)
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `subproject clean removes only artifacts matching its fingerprints`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            val lib1Fingerprints = Fingerprints(packageFingerprint = "lib1-package", xcodebuildFingerprint = "lib1-xcodebuild")
            val lib2Fingerprints = Fingerprints(packageFingerprint = "lib2-package", xcodebuildFingerprint = "lib2-xcodebuild")
            val lib1 = cleanupProject(version, lib1Fingerprints)
            val lib2 = cleanupProject(version, lib2Fingerprints)
            include(lib1, "lib1")
            include(lib2, "lib2")

            val lib1Artifacts = createSharedArtifacts(lib1Fingerprints)
            val lib2Artifacts = createSharedArtifacts(lib2Fingerprints)
            build(":lib1:clean")

            lib1Artifacts.forEach(::assertDirectoryDoesNotExist)
            lib2Artifacts.forEach(::assertDirectoryExists)
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `root clean removes artifacts matching all subproject fingerprints`(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            val lib1Fingerprints = Fingerprints(packageFingerprint = "lib1-package", xcodebuildFingerprint = "lib1-xcodebuild")
            val lib2Fingerprints = Fingerprints(packageFingerprint = "lib2-package", xcodebuildFingerprint = "lib2-xcodebuild")
            val lib1 = cleanupProject(version, lib1Fingerprints)
            val lib2 = cleanupProject(version, lib2Fingerprints)
            include(lib1, "lib1")
            include(lib2, "lib2")

            val sharedArtifacts = createSharedArtifacts(lib1Fingerprints) + createSharedArtifacts(lib2Fingerprints)
            build("clean")

            sharedArtifacts.forEach(::assertDirectoryDoesNotExist)
        }
    }

    private fun TestProject.cleanupProject(
        version: GradleVersion,
        fingerprints: Fingerprints,
    ): TestProject = project("empty", version) {
        plugins {
            kotlin("multiplatform").apply(false)
        }

        val syntheticPackageFingerprint = projectPath.resolve("build/testFingerprints/syntheticPackage")
        val xcodebuildFingerprint = projectPath.resolve("build/testFingerprints/xcodebuild")
        syntheticPackageFingerprint.parent.createDirectories()
        syntheticPackageFingerprint.writeFingerprint(fingerprints.packageFingerprint)
        xcodebuildFingerprint.writeFingerprint(fingerprints.xcodebuildFingerprint)

        buildScriptInjection {
            project.pluginManager.apply("base")
            val coordinationService = SwiftImportFingerprintedCoordinationService.registerIfAbsent(
                project = project,
                xcodeDumpsDir = project.layout.dir(project.provider { project.rootDir.resolve(SHARED_XCODE_DUMP_DIR) }),
                checkoutDir = project.layout.dir(project.provider { project.rootDir.resolve(SHARED_CHECKOUT_DIR) }),
                generatePackageDir = project.layout.dir(project.provider { project.rootDir.resolve(SHARED_SYNTHETIC_PACKAGE_DIR) }),
            )
            val cleanup = project.tasks.register<CleanSwiftImportFingerprintArtifacts>(
                CleanSwiftImportFingerprintArtifacts.TASK_NAME,
            ) {
                this.syntheticPackageFingerprint.set(project.layout.projectDirectory.file("build/testFingerprints/syntheticPackage"))
                xcodebuildFingerprints.from(project.layout.projectDirectory.file("build/testFingerprints/xcodebuild"))
                this.coordinationService.set(coordinationService)
            }
            project.tasks.named("clean").configure {
                it.dependsOn(cleanup)
            }
        }
    }

    private fun TestProject.createSharedArtifacts(fingerprints: Fingerprints): List<Path> = listOf(
        projectPath.resolve(SHARED_SYNTHETIC_PACKAGE_DIR).resolve(fingerprints.packageFingerprint),
        projectPath.resolve(SHARED_CHECKOUT_DIR).resolve(fingerprints.packageFingerprint),
        projectPath.resolve(SHARED_XCODE_DUMP_DIR).resolve(fingerprints.xcodebuildFingerprint),
    ).onEach { it.createDirectories() }

    private data class Fingerprints(
        val packageFingerprint: String,
        val xcodebuildFingerprint: String,
    )


}
