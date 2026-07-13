/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalKotlinGradlePluginApi::class, EnvironmentalVariablesOverride::class)

package org.jetbrains.kotlin.gradle.apple

import com.android.build.gradle.internal.tasks.factory.dependsOn
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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.locateOrRegisterSwiftPMDependenciesExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SyntheticProductType
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariables
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertDirectoryExists
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertFileNotExists
import org.jetbrains.kotlin.gradle.testbase.assertFilesExist
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@SwiftPMImportGradlePluginTests
class ConvertSyntheticSwiftPMImportProjectIntoDefFileTests : KGPBaseTest() {

    @GradleTest
    fun `smoke test - clang dump, ld dump and modules discovery - Swift library`(version: GradleVersion) {
        smokeTestClangAndLdDumpsAndModulesDiscovery(
            version,
            packageSetup = { packageDir ->
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageDir)
            },
            assertLinkerArguments = {
                // Since Xcode 27, products with Swift are linked by the swiftc driver
                if (Xcode.findCurrent().version.major > 26) {
                    assertSwiftcLdDump(it)
                } else {
                    assertClangLdDump(it)
                }
            },
            assertFrameworkLinkerArguments = {
                if (Xcode.findCurrent().version.major > 26) {
                    assertFrameworkSwiftcLdDump(it)
                } else {
                    assertFrameworkClangLdDump(it)
                }
            },
        )
    }

    @GradleTest
    fun `smoke test - clang dump, ld dump and modules discovery - Clang library`(version: GradleVersion) {
        smokeTestClangAndLdDumpsAndModulesDiscovery(
            version,
            packageSetup = { packageDir ->
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageDir)
                packageDir.resolve("Sources").resolve(packageDir.name).resolve("${packageDir.name}.swift").delete()
                packageDir.resolve("Sources").resolve(packageDir.name).resolve("${packageDir.name}.m").writeText("")
                packageDir.resolve("Sources").resolve(packageDir.name).resolve("include/${packageDir.name}.h").also {
                    it.parentFile.mkdirs()
                }.writeText("")
            },
            assertLinkerArguments = {
                assertClangLdDump(it)
            },
            assertFrameworkLinkerArguments = {
                assertFrameworkClangLdDump(it)
            }
        )
    }

    @GradleTest
    fun `ld dump - resp file handling`(version: GradleVersion) {
        project("empty", version) {
            registerConvertTask { packageDir ->
                runProcess(listOf("swift", "package", "init", "--type", "library"), packageDir)
            }

            val uikitCount = 10000
            val induceRespFile = projectPath.resolve("foo.xcconfig")
            induceRespFile.writeText("""
                OTHER_LDFLAGS=${"-framework UIKit ".repeat(uikitCount)} 
                OTHER_CFLAGS=${"-framework UIKit ".repeat(uikitCount)}
            """.trimIndent())

            buildAndFail("packageDump", environmentVariables = EnvironmentalVariables(
                mapOf(
                    "XCODE_XCCONFIG_FILE" to induceRespFile.pathString,
                    "FOLLOW_RESP_FILES" to "false",
                )
            )) {
                // Resp files seem to only be used by the clang/swiftc drivers. Xcodebuild doesn't itself seem to use resp files to call clang
                assertOutputContains("NoLinkerCallsDiscovered")
            }

            build("packageDump", environmentVariables = EnvironmentalVariables(
                mapOf(
                    "XCODE_XCCONFIG_FILE" to induceRespFile.pathString,
                )
            ))

            assertClangDump(projectPath)

            val ldDump = dumpLinkerArguments(projectPath.resolve("build/kotlin/swiftImportLdDump/iphonesimulator/arm64.ld"))
            if (Xcode.findCurrent().version.major > 26) {
                assertSwiftcLdDump(ldDump)
            } else {
                assertClangLdDump(ldDump)
            }

            assertEquals(
                uikitCount,
                ldDump.linkerArguments.count { it == "UIKit" }
            )
        }
    }

    private fun assertClangLdDump(ldDump: LdDump) {
        assertFilesExist(*ldDump.fileLists.map { Path.of(it) }.toTypedArray())
        assertEquals(ldDump.fileLists.count(), 1, message = "${ldDump.fileLists}")
        assertEquals(ldDump.objectFiles.count(), 0, message = "${ldDump.objectFiles}")
        assertEquals(listOf("libclang_rt.iossim.a", "KotlinMultiplatformLinkedPackageDylib.LinkFileList"), ldDump.fileArguments)
    }

    private fun assertSwiftcLdDump(ldDump: LdDump) {
        assertFilesExist(*ldDump.objectFiles.map { Path.of(it) }.toTypedArray())
        assertEquals(
            listOf("KotlinMultiplatformLinkedPackageDylib.o", "packageOne.o"),
            ldDump.objectFiles.map { Path.of(it).name },
        )
        assertEquals(emptyList(), ldDump.fileLists)
        assertEquals(listOf("libswiftCompatibility56.a", "libswiftCompatibilityPacks.a", "libclang_rt.iossim.a"), ldDump.fileArguments)
    }

    private fun assertFrameworkClangLdDump(ldDump: LdDump) {
        assertEquals(emptyList(), ldDump.objectFiles.map { Path.of(it).name })
        assertEquals(emptyList(), ldDump.fileLists)
        assertEquals(listOf("KotlinMultiplatformLinkedPackageDylib", "libclang_rt.iossim.a"), ldDump.fileArguments)
    }

    private fun assertFrameworkSwiftcLdDump(ldDump: LdDump) {
        assertEquals(emptyList(), ldDump.objectFiles.map { Path.of(it).name })
        assertEquals(emptyList(), ldDump.fileLists)
        assertEquals(listOf("KotlinMultiplatformLinkedPackageDylib", "libswiftCompatibility56.a", "libswiftCompatibilityPacks.a", "libclang_rt.iossim.a"), ldDump.fileArguments)
    }

    data class LdDump(
        val objectFiles: List<String>,
        val fileLists: List<String>,
        val fileArguments: List<String>,
        val linkerArguments: List<String>,
    )

    private fun assertClangDump(projectPath: Path) {
        val generatedDefFile = projectPath.resolve("build/kotlin/swiftImportDefs/iphonesimulator/arm64.def").toFile()
        assertFileExists(generatedDefFile)
        val compilerOpts = generatedDefFile.readLines().single { it.startsWith("compilerOpts") }

        val packageDependencyModule =
            projectPath.resolve("build/kotlin/swiftImportDd/dd_iphonesimulator/Build/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator/packageOne.modulemap")
        assertFileExists(packageDependencyModule)
        // We must have discovered the modulmap file reference to be able to index it with cinterops
        assertContains(compilerOpts, "-fmodule-map-file=${packageDependencyModule.pathString}")

        val sharedDerivedDataSearchPath =
            projectPath.resolve("build/kotlin/swiftImportDd/dd_iphonesimulator/Build/Products/Debug-iphonesimulator")
        assertDirectoryExists(sharedDerivedDataSearchPath)
        // This is the general -I/-F search path, we expect to always be to discover it from the dump
        assertContains(compilerOpts, "-I${sharedDerivedDataSearchPath.pathString}")
        assertContains(compilerOpts, "-F${sharedDerivedDataSearchPath.pathString}")

        val discoveredModules = generatedDefFile.readLines().single { it.startsWith("modules") }
        assertEquals("modules = \"packageOne\"", discoveredModules)
    }

    private fun TestProject.registerConvertTask(packageSetup: (File) -> Unit) {
        val packageOne = projectPath.resolve("packageOne").also { it.createDirectories() }.toFile()
        packageSetup(packageOne)

        plugins {
            kotlin("multiplatform").apply(false)
        }
        val stubTrackedFiles = projectPath.resolve("trackedFilesStub").also { it.createFile() }.toFile()
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
                transitiveSwiftPMMetadata.set(TransitiveSwiftPMMetadata(emptyMap()))
                syntheticProductType.set(SyntheticProductType.DYNAMIC)
            }

            val packageBuild = project.tasks.register<DumpXcodeBuildArgs>("packageBuild") {
                dependsOn(packageGeneration)
                xcodebuildSdk.set(KonanTarget.IOS_SIMULATOR_ARM64.appleTarget.sdk)
                xcodebuildPlatform.set(KonanTarget.IOS_SIMULATOR_ARM64.applePlatform)
                architectures.set(setOf(KonanTarget.IOS_SIMULATOR_ARM64.appleArchitecture))
                swiftPMDependenciesCheckout.set(project.layout.buildDirectory.dir("checkout"))
                syntheticImportProjectRoot.set(packageGeneration.map { it.syntheticImportProjectRoot.get() })
                ideaSyncEnabled.set(false)
                inputs.property("env", project.providers.environmentVariable("FOLLOW_RESP_FILES").orElse("true"))
            }

            project.tasks.register<ConvertSyntheticSwiftPMImportProjectIntoDefFile>("packageDump") {
                dependsOn(packageBuild)
                xcodebuildSdk.set(KonanTarget.IOS_SIMULATOR_ARM64.appleTarget.sdk)
                architectures.add(KonanTarget.IOS_SIMULATOR_ARM64.appleArchitecture)
                discoverModulesImplicitly.set(true)
                hasSwiftPMDependencies.set(true)
                localPackages.filesToTrackFromLocalPackages.set(stubTrackedFiles)
                ideaSyncEnabled.set(false)
            }
        }
    }

    private fun dumpLinkerArguments(ldDumpPath: Path): LdDump {
        assertFileExists(ldDumpPath)

        // Product dependencies from sources-based targets will be passed in the filelist, so check that we were able to discover some filelist
        val linkerArguments = ldDumpPath.readLines().single().split(';')

        fun dumpFileLists(args: List<String>) = args.indices.filter {
            args[it] == "-filelist"
        }.map { args[it + 1] }
        fun dumpObjectFiles(args: List<String>) = args.filter { it.endsWith(".o") }
        fun dumpFileArguments(args: List<String>) = args.indices.filter {
            args[it].startsWith("/")
                    && !args[it].endsWith(".o")
                    && (if (it > 0) args[it-1] !in setOf("-lto_library") else true)
        }.map { File(args[it]).name }

        val objectFiles = dumpObjectFiles(linkerArguments)
        val fileLists = dumpFileLists(linkerArguments)
        val fileArguments = dumpFileArguments(linkerArguments)

        return LdDump(
            objectFiles = objectFiles,
            fileLists = fileLists,
            fileArguments = fileArguments,
            linkerArguments = linkerArguments,
        )
    }

    private fun smokeTestClangAndLdDumpsAndModulesDiscovery(
        version: GradleVersion,
        dumpEnv: Map<String, String> = mapOf(),
        packageSetup: (File) -> Unit,
        assertLinkerArguments: (LdDump) -> Unit,
        assertFrameworkLinkerArguments: (LdDump) -> Unit,
    ) {
        project("empty", version) {
            registerConvertTask(packageSetup)

            build("packageDump", environmentVariables = EnvironmentalVariables(dumpEnv))

            assertClangDump(projectPath)

            assertLinkerArguments(
                dumpLinkerArguments(projectPath.resolve("build/kotlin/swiftImportLdDump/iphonesimulator/arm64.ld"))
            )
            assertFrameworkLinkerArguments(
                dumpLinkerArguments(projectPath.resolve("build/kotlin/swiftImportLdDump/iphonesimulator/arm64.framework.ld"))
            )
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
