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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.SyntheticProductType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
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
        project("empty", version) {
            val stubTrackedFiles = projectPath.resolve("trackedFilesStub").also { it.createFile() }.toFile()
            val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
            runProcess(listOf("swift", "package", "init", "--type", "library"), packageOne)

            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.createKotlinExtension(KotlinMultiplatformExtension::class)
                val extension = project.locateOrRegisterSwiftPMDependenciesExtension().apply {
                    localSwiftPackage(
                        directory = project.layout.projectDirectory.dir("packageOne"),
                        products = listOf("packageOne"),
                    )
                }
                val packageGeneration = project.tasks.register<GenerateSyntheticLinkageImportProject>("packageGeneration") {
                    configureWithExtension(extension)
                    konanTargets.set(setOf(KonanTarget.IOS_SIMULATOR_ARM64))
                    dependencyIdentifierToImportedSwiftPMDependencies.set(TransitiveSwiftPMDependencies(emptyMap()))
                    syntheticProductType.set(SyntheticProductType.DYNAMIC)
                }

                project.tasks.register<ConvertSyntheticSwiftPMImportProjectIntoDefFile>("packageDump") {
                    dependsOn(packageGeneration)
                    xcodebuildPlatform.set(KonanTarget.IOS_SIMULATOR_ARM64.applePlatform)
                    xcodebuildSdk.set(KonanTarget.IOS_SIMULATOR_ARM64.appleTarget.sdk)
                    architectures.set(setOf(KonanTarget.IOS_SIMULATOR_ARM64.appleArchitecture))
                    clangModules.set(emptySet())
                    discoverModulesImplicitly.set(true)
                    hasSwiftPMDependencies.set(true)
                    filesToTrackFromLocalPackages.set(stubTrackedFiles)
                    swiftPMDependenciesCheckout.set(project.layout.buildDirectory.dir("checkout"))
                    syntheticImportProjectRoot.set(packageGeneration.map { it.syntheticImportProjectRoot.get() })
                }
            }

            build("packageDump")

            val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/iphonesimulator/arm64.def").toFile()
            assertFileExists(generatedDefFile)
            val compilerOpts = generatedDefFile.readLines().single { it.startsWith("compilerOpts") }

            val packageDependencyModule = projectPath.resolve("build/kotlin/swiftImportDd/dd_iphonesimulator/Build/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator/packageOne.modulemap")
            assertFileExists(packageDependencyModule)
            // We must have discovered the modulmap file reference to be able to index it with cinterops
            assertContains(compilerOpts, "-fmodule-map-file=${packageDependencyModule.pathString}")

            val sharedDerivedDataSearchPath = projectPath.resolve("build/kotlin/swiftImportDd/dd_iphonesimulator/Build/Products/Debug-iphonesimulator")
            assertDirectoryExists(sharedDerivedDataSearchPath)
            // This is the general -I/-F search path, we expect to always be to discover it from the dump
            assertContains(compilerOpts, "-I${sharedDerivedDataSearchPath.pathString}")
            assertContains(compilerOpts, "-F${sharedDerivedDataSearchPath.pathString}")

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
