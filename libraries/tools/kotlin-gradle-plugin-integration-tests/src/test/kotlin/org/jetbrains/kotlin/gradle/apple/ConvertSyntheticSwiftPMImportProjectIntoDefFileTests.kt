/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.register
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.createKotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertFileNotExists
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class ConvertSyntheticSwiftPMImportProjectIntoDefFileTests : KGPBaseTest() {

    @GradleTest
    fun `smoke test - clang dump, ld dump and modules discovery`(version: GradleVersion) {
    }
    
    private fun smokeTestClangAndLdDumpsAndModulesDiscovery() {
        
    }

    @GradleTest
    fun `sdk without relevant SwiftPM dependencies writes stub outputs without xcodebuild`(version: GradleVersion) {
        project("empty", version) {
            val stubTrackedFiles = projectPath.resolve("trackedFilesStub").also { it.createFile() }.toFile()

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                project.tasks.register<ConvertSyntheticSwiftPMImportProjectIntoDefFile>("packageDump") {
                    xcodebuildPlatform.set(KonanTarget.MACOS_ARM64.applePlatform)
                    xcodebuildSdk.set(KonanTarget.MACOS_ARM64.appleTarget.sdk)
                    architectures.add(KonanTarget.MACOS_ARM64.appleArchitecture)
                    discoverModulesImplicitly.set(true)
                    hasSwiftPMDependencies.set(false)
                    ideaSyncEnabled.set(false)
                    filesToTrackFromLocalPackages.set(stubTrackedFiles)
                    swiftPMDependenciesCheckout.set(project.layout.buildDirectory.dir("checkout"))
                    syntheticImportProjectRoot.set(project.layout.buildDirectory.dir("unusedSyntheticProject"))
                }
            }

            build("packageDump")

            val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/macosx/arm64.def").toFile()
            assertFileExists(generatedDefFile)
            assertEquals(
                listOf("language = Objective-C", "package = swiftPMImport.empty"),
                generatedDefFile.readLines(),
            )
            assertFileExists(projectPath.resolve("build/kotlin/swiftImportLdDump/macosx/arm64.ld"))
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    @GradleTest
    fun `KT-86174 - convertSyntheticImportProjectIntoDefFile tasks re-execute fetch after clean`(
        version: GradleVersion,
    ) {
        val convertTaskNames = arrayOf(
            "convertSyntheticImportProjectIntoDefFileIphoneos",
            "convertSyntheticImportProjectIntoDefFileIphonesimulator",
        )

        project("empty", version) {
            withLockFileFixture {
                val repoName = "TestPackageA"
                val repo = repoRef(repoName).also {
                    createRepo(it.name, listOf("1.0.0"))
                }

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        swiftPackage(
                            url = url(repo.url),
                            version = from("1.0.0"),
                            products = listOf(product(repo.name)),
                        )
                    }
                }

                val rootCheckout = projectPath.resolve("build/kotlin/swiftPMCheckout")
                val rootFetchTask = ":${FetchSyntheticImportProjectPackages.TASK_NAME}"

                build(*convertTaskNames) {
                    assertTasksExecuted(rootFetchTask)
                    assertDirectoryExists(rootCheckout)
                }

                build("clean")
                assertFileNotExists(rootCheckout)

                build(*convertTaskNames) {
                    assertTasksExecuted(rootFetchTask)
                    assertDirectoryExists(rootCheckout)
                }
            }
        }
    }

}
