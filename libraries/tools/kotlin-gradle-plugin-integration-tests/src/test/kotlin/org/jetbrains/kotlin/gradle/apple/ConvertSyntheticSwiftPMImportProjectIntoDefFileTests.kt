/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
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
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.io.path.createDirectories
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
        project("empty", version) {
            withLockFileFixture {
                val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageOne)

                initSwiftPmProject(cacheDirFile) {
                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("packageOne"),
                            products = listOf("packageOne"),
                        )
                    }
                }

                buildScriptInjection {
                    val task = project.tasks.findByPath(":convertSyntheticImportProjectIntoDefFileIphonesimulator")
                            as ConvertSyntheticSwiftPMImportProjectIntoDefFile

                    task.ideaSyncEnabled.set(false)
                }

                build("convertSyntheticImportProjectIntoDefFileIphonesimulator")

                val derivedData = localDerivedDataDir(
                    sdk = "iphonesimulator",
                )

                val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/iphonesimulator/arm64.def").toFile()
                assertFileExists(generatedDefFile)
                val compilerOpts = generatedDefFile.readLines().single { it.startsWith("compilerOpts") }

                // On macOS, /var is a symlink to /private/var. Xcode's dump output captures the resolved path,
                // so we must call toRealPath() to normalize the symlink before comparing with compiler opts.
                val packageDependencyModule =
                    derivedData
                        .resolve("Build/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator/packageOne.modulemap")

                assertFileExists(packageDependencyModule)

                assertContains(
                    normalizeMacTempPath(compilerOpts),
                    "-fmodule-map-file=${normalizeMacTempPath(packageDependencyModule.pathString)}"
                )

                val sharedDerivedDataSearchPath =
                    derivedData
                        .resolve("Build/Products/Debug-iphonesimulator")

                assertDirectoryExists(sharedDerivedDataSearchPath)
                // This is the general -I/-F search path, we expect to always be to discover it from the dump
                assertContains(
                    normalizeMacTempPath(compilerOpts),
                    "-I${normalizeMacTempPath(sharedDerivedDataSearchPath.pathString)}"
                )
                assertContains(
                    normalizeMacTempPath(compilerOpts),
                    "-F${normalizeMacTempPath(sharedDerivedDataSearchPath.pathString)}"
                )

                val discoveredModules = generatedDefFile.readLines().single { it.startsWith("modules") }
                assertEquals("modules = \"packageOne\"", discoveredModules)

                val ldDumpPath = projectPath.resolve("build/kotlin/swiftImportLdDump/iphonesimulator/arm64.ld")
                assertFileExists(ldDumpPath)

                // Product dependencies from sources-based targets will be passed in the filelist, so check that we were able to discover some filelist
                val linkerArguments = ldDumpPath.readLines().single().split(';')
                val fileList = linkerArguments[linkerArguments.indices.single { linkerArguments[it] == "-filelist" } + 1]
                assertFileExists(File(fileList))
            }
        }
    }

    @GradleTest
    fun `sdk without relevant SwiftPM dependencies writes stub outputs without xcodebuild`(version: GradleVersion) {
        project("empty", version) {
            withLockFileFixture {
                initSwiftPmProject(cacheDirFile){}

                buildScriptInjection {
                    val task = project.tasks.findByPath(":convertSyntheticImportProjectIntoDefFileIphonesimulator")
                            as ConvertSyntheticSwiftPMImportProjectIntoDefFile

                    task.ideaSyncEnabled.set(false)
                }

                build("convertSyntheticImportProjectIntoDefFileIphoneos")


                val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/iphoneos/arm64.def").toFile()
                assertFileExists(generatedDefFile)
                assertEquals(
                    listOf("language = Objective-C", "package = swiftPMImport.empty"),
                    generatedDefFile.readLines(),
                )
                assertFileExists(projectPath.resolve("build/kotlin/swiftImportLdDump/iphoneos/arm64.ld"))
            }
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


private fun normalizeMacTempPath(path: String): String =
    path.replace("/private/var/", "/var/")
