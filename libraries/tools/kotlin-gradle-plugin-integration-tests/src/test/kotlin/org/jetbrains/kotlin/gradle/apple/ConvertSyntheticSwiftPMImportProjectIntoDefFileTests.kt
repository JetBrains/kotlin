/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.build
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

                build("convertSyntheticImportProjectIntoDefFileIphonesimulator")

                val xcodeDumpLocation: TestXcodeDumpLocation = parseXcodeDumpLocationFile(
                    projectPath.resolve("build/kotlin/swiftPMXcodeDumpLocations/iphonesimulator.json")
                )

                val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/iphonesimulator/arm64.def").toFile()
                assertFileExists(generatedDefFile)
                val compilerOpts = generatedDefFile.readLines().single { it.startsWith("compilerOpts") }

                // On macOS, /var is a symlink to /private/var. Xcode's dump output captures the resolved path,
                // so we must call toRealPath() to normalize the symlink before comparing with compiler opts.
                val packageDependencyModule =
                    File(xcodeDumpLocation.derivedDataDir)
                        .resolve("Build/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator/packageOne.modulemap")

                assertFileExists(packageDependencyModule)

                assertContains(
                    normalizeMacTempPath(compilerOpts),
                    "-fmodule-map-file=${normalizeMacTempPath(packageDependencyModule.path)}"
                )

                val sharedDerivedDataSearchPath =
                    File(xcodeDumpLocation.derivedDataDir)
                        .resolve("Build/Products/Debug-iphonesimulator")
                        .toPath()

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

}


private fun normalizeMacTempPath(path: String): String =
    path.replace("/private/var/", "/var/")
